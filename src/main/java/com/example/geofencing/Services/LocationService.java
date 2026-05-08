package com.example.geofencing.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.geofencing.Bootstrap.LocationWebSocketHandler;
import com.example.geofencing.Components.GeometryUtils;
import com.example.geofencing.DTO.GeofenceEventDto;
import com.example.geofencing.DTO.KafkaGeofenceEvent;
import com.example.geofencing.DTO.KafkaLocationEvent;
import com.example.geofencing.DTO.LocationDto;
import com.example.geofencing.Entities.Geofence;
import com.example.geofencing.Entities.GeofenceEvent;
import com.example.geofencing.Entities.LocationUpdate;
import com.example.geofencing.Entities.TrackingSession;
import com.example.geofencing.Entities.User;
import com.example.geofencing.Enums.AlertSeverity;
import com.example.geofencing.Enums.AlertType;
import com.example.geofencing.Enums.GeofenceEventType;
import com.example.geofencing.Enums.TrackingStatus;
import com.example.geofencing.Exceptions.GeofencingException;
import com.example.geofencing.Reponses.PagedResponse;
import com.example.geofencing.Repositories.GeofenceEventRepository;
import com.example.geofencing.Repositories.GeofenceRepository;
import com.example.geofencing.Repositories.LocationUpdateRepository;
import com.example.geofencing.Repositories.TrackingSessionRepository;
import com.example.geofencing.Repositories.UserRepository;
import com.example.geofencing.kafka.KafkaProducerService;
import java.time.LocalDateTime;
import java.util.*;

import com.example.geofencing.Payloads.LocationUpdateRequest;
 
@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {
 
    private final LocationUpdateRepository locationUpdateRepository;
    private final GeofenceRepository geofenceRepository;
    private final GeofenceEventRepository geofenceEventRepository;
    private final UserRepository userRepository;
    private final TrackingSessionRepository trackingSessionRepository;
    private final CacheService cacheService;
    private final KafkaProducerService kafkaProducerService;
    private final AlertService alertService;
    private final LocationWebSocketHandler webSocketHandler;
 
    @Value("${geofencing.location.history-ttl-hours:24}") private long historyTtlHours;
    @Value("${geofencing.location.max-history-per-user:1000}") private int maxHistoryPerUser;
    @Value("${geofencing.cache.location-ttl-seconds:30}") private long locationCacheTtl;
 
    @Transactional
    public LocationDto processLocationUpdate(UUID userId, LocationUpdateRequest request) {
        var user = userRepository.findById(userId)
            .orElseThrow(() -> GeofencingException.notFound("User"));
 
        double lat = request.getLatitude();
        double lng = request.getLongitude();
 
        // Persist location update
        var locationUpdate = LocationUpdate.builder()
            .user(user)
            .location(GeometryUtils.createPoint(lat, lng))
            .latitude(lat)
            .longitude(lng)
            .altitude(request.getAltitude())
            .accuracy(request.getAccuracy())
            .speed(request.getSpeed())
            .heading(request.getHeading())
            .batteryLevel(request.getBatteryLevel())
            .deviceId(request.getDeviceId())
            .provider(request.getProvider())
            .timestamp(request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now())
            .build();
 
        locationUpdate = locationUpdateRepository.save(locationUpdate);
 
        // Cache location
        var locationDto = toLocationDto(locationUpdate);
        cacheService.cacheUserLocation(userId.toString(), locationDto, locationCacheTtl);
        cacheService.pushLocationHistory(userId.toString(), locationDto, maxHistoryPerUser);
 
        // Update tracking session stats
        updateTrackingSession(userId, request.getDeviceId(), lat, lng);
 
        // Process geofence events
        List<LocationDto.ActiveGeofenceDto> activeGeofences = processGeofenceTransitions(user, lat, lng, locationUpdate);
        locationDto.setActiveGeofences(activeGeofences);
 
        // Publish to Kafka
        kafkaProducerService.publishLocationUpdate(KafkaLocationEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .userId(userId)
            .latitude(lat)
            .longitude(lng)
            .altitude(request.getAltitude())
            .speed(request.getSpeed())
            .heading(request.getHeading())
            .batteryLevel(request.getBatteryLevel())
            .deviceId(request.getDeviceId())
            .timestamp(locationUpdate.getTimestamp())
            .build());
 
        // Push via WebSocket
        webSocketHandler.sendLocationUpdate(userId.toString(), locationDto);
 
        // Battery alert
        if (request.getBatteryLevel() != null && request.getBatteryLevel() < 15) {
            alertService.createAlert(user, null, null,
                AlertType.BATTERY_LOW, AlertSeverity.WARNING,
                "Low Battery", "Battery level is " + request.getBatteryLevel() + "%", null);
        }
 
        // Speed alert (if > 200 km/h — configurable)
        if (request.getSpeed() != null && request.getSpeed() > 55.5) { // ~200 km/h in m/s
            alertService.createAlert(user, null, null,
                AlertType.SPEED_ALERT, AlertSeverity.WARNING,
                "High Speed Detected",
                String.format("Speed %.1f km/h detected", request.getSpeed() * 3.6), null);
        }
 
        return locationDto;
    }
 
    private List<LocationDto.ActiveGeofenceDto> processGeofenceTransitions(
            User user, double lat, double lng, LocationUpdate locationUpdate) {
 
        String userId = user.getId().toString();
        List<LocationDto.ActiveGeofenceDto> activeGeofences = new ArrayList<>();
 
        // Get all active geofences relevant to this user
        List<Geofence> allUserGeofences = geofenceRepository.findContainingPointForUser(lat, lng, user.getId());
        Set<String> currentlyInsideIds = new HashSet<>();
 
        for (Geofence geofence : allUserGeofences) {
            String geofenceId = geofence.getId().toString();
            currentlyInsideIds.add(geofenceId);
 
            boolean wasInside = cacheService.getUserGeofenceState(userId, geofenceId).orElse(false);
 
            if (!wasInside) {
                // ENTER event
                handleEnterEvent(user, geofence, lat, lng, locationUpdate);
            } else {
                // Still inside — check dwell
                handleDwellCheck(user, geofence, lat, lng, locationUpdate);
            }
 
            cacheService.setUserGeofenceState(userId, geofenceId, true, historyTtlHours * 3600);
            cacheService.addToActiveGeofences(userId, geofenceId);
 
            Double distance = geofenceRepository.getDistanceToGeofence(geofence.getId(), lat, lng);
            activeGeofences.add(LocationDto.ActiveGeofenceDto.builder()
                .geofenceId(geofence.getId())
                .name(geofence.getName())
                .inside(true)
                .distanceToBoundaryMeters(distance != null ? distance : 0)
                .build());
        }
 
        // Check for EXIT events
        Set<Object> previousGeofenceIds = cacheService.getActiveGeofencesForUser(userId);
        for (Object prevId : previousGeofenceIds) {
            if (!currentlyInsideIds.contains(prevId.toString())) {
                geofenceRepository.findById(UUID.fromString(prevId.toString())).ifPresent(geofence -> {
                    handleExitEvent(user, geofence, lat, lng, locationUpdate);
                    cacheService.setUserGeofenceState(userId, prevId.toString(), false, historyTtlHours * 3600);
                    cacheService.removeFromActiveGeofences(userId, prevId.toString());
                    cacheService.clearDwellTimer(userId, prevId.toString());
                });
            }
        }
 
        return activeGeofences;
    }
 
    private void handleEnterEvent(User user, Geofence geofence, double lat, double lng, LocationUpdate locationUpdate) {
        if (!geofence.getEnterAlert()) return;
 
        var event = saveGeofenceEvent(user, geofence, GeofenceEventType.ENTER, lat, lng, null);
        cacheService.startDwellTimer(user.getId().toString(), geofence.getId().toString());
 
        alertService.createAlert(user, geofence, event,
            AlertType.GEOFENCE_ENTER, AlertSeverity.INFO,
            "Entered " + geofence.getName(),
            "You entered the geofenced area: " + geofence.getName(), null);
 
        kafkaProducerService.publishGeofenceEvent(KafkaGeofenceEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .userId(user.getId())
            .geofenceId(geofence.getId())
            .geofenceName(geofence.getName())
            .eventType(GeofenceEventType.ENTER)
            .latitude(lat).longitude(lng)
            .occurredAt(LocalDateTime.now())
            .build());
 
        log.debug("User {} ENTERED geofence {}", user.getId(), geofence.getName());
    }
 
    private void handleExitEvent(User user, Geofence geofence, double lat, double lng, LocationUpdate locationUpdate) {
        if (!geofence.getExitAlert()) return;
 
        Optional<Long> dwellStart = cacheService.getDwellStartTime(
            user.getId().toString(), geofence.getId().toString());
        Integer dwellSeconds = dwellStart.map(start ->
            (int) ((System.currentTimeMillis() - start) / 1000)).orElse(null);
 
        var event = saveGeofenceEvent(user, geofence, GeofenceEventType.EXIT, lat, lng, dwellSeconds);
 
        alertService.createAlert(user, geofence, event,
            AlertType.GEOFENCE_EXIT, AlertSeverity.INFO,
            "Exited " + geofence.getName(),
            "You exited the geofenced area: " + geofence.getName(), null);
 
        kafkaProducerService.publishGeofenceEvent(KafkaGeofenceEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .userId(user.getId())
            .geofenceId(geofence.getId())
            .geofenceName(geofence.getName())
            .eventType(GeofenceEventType.EXIT)
            .latitude(lat).longitude(lng)
            .dwellDurationSeconds(dwellSeconds)
            .occurredAt(LocalDateTime.now())
            .build());
 
        log.debug("User {} EXITED geofence {}", user.getId(), geofence.getName());
    }
 
    private void handleDwellCheck(User user, Geofence geofence, double lat, double lng, LocationUpdate locationUpdate) {
        if (!geofence.getDwellAlert()) return;
 
        Optional<Long> dwellStart = cacheService.getDwellStartTime(
            user.getId().toString(), geofence.getId().toString());
 
        dwellStart.ifPresent(start -> {
            long dwellSeconds = (System.currentTimeMillis() - start) / 1000;
            if (dwellSeconds >= geofence.getDwellTimeSeconds()) {
                // Check if we already fired dwell event recently
                String dwellFiredKey = "dwell:fired:" + user.getId() + ":" + geofence.getId();
                boolean alreadyFired = cacheService.get(dwellFiredKey).isPresent();
 
                if (!alreadyFired) {
                    var event = saveGeofenceEvent(user, geofence, GeofenceEventType.DWELL,
                        lat, lng, (int) dwellSeconds);
 
                    alertService.createAlert(user, geofence, event,
                        AlertType.GEOFENCE_DWELL, AlertSeverity.INFO,
                        "Dwelling in " + geofence.getName(),
                        String.format("You have been in %s for %d minutes",
                            geofence.getName(), dwellSeconds / 60), null);
 
                    // Set fired flag — reset after dwell_time_seconds to allow re-fire
                    cacheService.set(dwellFiredKey, true, geofence.getDwellTimeSeconds());
 
                    kafkaProducerService.publishGeofenceEvent(KafkaGeofenceEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .userId(user.getId())
                        .geofenceId(geofence.getId())
                        .geofenceName(geofence.getName())
                        .eventType(GeofenceEventType.DWELL)
                        .latitude(lat).longitude(lng)
                        .dwellDurationSeconds((int) dwellSeconds)
                        .occurredAt(LocalDateTime.now())
                        .build());
                }
            }
        });
    }
 
    private GeofenceEvent saveGeofenceEvent(User user, Geofence geofence, GeofenceEventType type,
                                             double lat, double lng, Integer dwellSeconds) {
        var event = GeofenceEvent.builder()
            .user(user)
            .geofence(geofence)
            .eventType(type)
            .location(GeometryUtils.createPoint(lat, lng))
            .latitude(lat)
            .longitude(lng)
            .dwellDurationSeconds(dwellSeconds)
            .build();
        return geofenceEventRepository.save(event);
    }
 
    private void updateTrackingSession(UUID userId, String deviceId, double lat, double lng) {
        trackingSessionRepository
            .findTopByUserIdAndStatusOrderByStartedAtDesc(userId, TrackingStatus.ACTIVE)
            .ifPresent(session -> {
                // Calculate distance from last known location
                cacheService.getUserLocation(userId.toString(), LocationDto.class).ifPresent(lastLoc -> {
                    double dist = GeometryUtils.distanceInMeters(
                        lastLoc.getLatitude(), lastLoc.getLongitude(), lat, lng);
                    if (dist < 50000) { // sanity check: < 50km jump
                        trackingSessionRepository.incrementStats(session.getId(), dist);
                    }
                });
            });
    }
 
    @Transactional
    public TrackingSession startTracking(UUID userId, String deviceId) {
        // End any active session
        trackingSessionRepository
            .findTopByUserIdAndStatusOrderByStartedAtDesc(userId, TrackingStatus.ACTIVE)
            .ifPresent(session -> {
                session.setStatus(TrackingStatus.ENDED);
                session.setEndedAt(LocalDateTime.now());
                trackingSessionRepository.save(session);
            });
 
        var user = userRepository.findById(userId)
            .orElseThrow(() -> GeofencingException.notFound("User"));
 
        var session = TrackingSession.builder()
            .user(user).deviceId(deviceId).build();
        session = trackingSessionRepository.save(session);
 
        alertService.createAlert(user, null, null,
            AlertType.TRACKING_STARTED, AlertSeverity.INFO,
            "Tracking Started", "Location tracking has been started", null);
 
        return session;
    }
 
    @Transactional
    public void stopTracking(UUID userId) {
        trackingSessionRepository
            .findTopByUserIdAndStatusOrderByStartedAtDesc(userId, TrackingStatus.ACTIVE)
            .ifPresent(session -> {
                session.setStatus(TrackingStatus.ENDED);
                session.setEndedAt(LocalDateTime.now());
                trackingSessionRepository.save(session);
            });
 
        userRepository.findById(userId).ifPresent(user ->
            alertService.createAlert(user, null, null,
                AlertType.TRACKING_STOPPED, AlertSeverity.INFO,
                "Tracking Stopped", "Location tracking has been stopped", null));
    }
 
    public Optional<LocationDto> getCurrentLocation(UUID userId) {
        // Try cache first
        Optional<LocationDto> cached = cacheService.getUserLocation(userId.toString(), LocationDto.class);
        if (cached.isPresent()) return cached;
 
        // Fallback to DB
        return locationUpdateRepository.findTopByUserIdOrderByTimestampDesc(userId)
            .map(this::toLocationDto);
    }
 
    public PagedResponse<LocationDto> getLocationHistory(UUID userId, LocalDateTime from,
                                                         LocalDateTime to, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var result = (from != null && to != null)
            ? locationUpdateRepository.findByUserIdAndTimeRange(userId, from, to, pageable)
            : locationUpdateRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
 
        return PagedResponse.<LocationDto>builder()
            .content(result.getContent().stream().map(this::toLocationDto).toList())
            .page(page).size(size)
            .totalElements(result.getTotalElements())
            .totalPages(result.getTotalPages())
            .hasNext(result.hasNext())
            .hasPrevious(result.hasPrevious())
            .build();
    }
 
    public PagedResponse<GeofenceEventDto> getGeofenceEvents(UUID userId, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var result = geofenceEventRepository.findByUserIdOrderByOccurredAtDesc(userId, pageable);
        return PagedResponse.<GeofenceEventDto>builder()
            .content(result.getContent().stream().map(this::toEventDto).toList())
            .page(page).size(size)
            .totalElements(result.getTotalElements())
            .totalPages(result.getTotalPages())
            .hasNext(result.hasNext())
            .hasPrevious(result.hasPrevious())
            .build();
    }
 
    private LocationDto toLocationDto(LocationUpdate loc) {
        return LocationDto.builder()
            .id(loc.getId())
            .userId(loc.getUser().getId())
            .latitude(loc.getLatitude())
            .longitude(loc.getLongitude())
            .altitude(loc.getAltitude())
            .accuracy(loc.getAccuracy())
            .speed(loc.getSpeed())
            .heading(loc.getHeading())
            .batteryLevel(loc.getBatteryLevel())
            .deviceId(loc.getDeviceId())
            .provider(loc.getProvider())
            .timestamp(loc.getTimestamp())
            .activeGeofences(new ArrayList<>())
            .build();
    }
 
    private GeofenceEventDto toEventDto(GeofenceEvent e) {
        return GeofenceEventDto.builder()
            .id(e.getId())
            .userId(e.getUser().getId())
            .geofenceId(e.getGeofence().getId())
            .geofenceName(e.getGeofence().getName())
            .eventType(e.getEventType())
            .latitude(e.getLatitude())
            .longitude(e.getLongitude())
            .dwellDurationSeconds(e.getDwellDurationSeconds())
            .occurredAt(e.getOccurredAt())
            .build();
    }
}