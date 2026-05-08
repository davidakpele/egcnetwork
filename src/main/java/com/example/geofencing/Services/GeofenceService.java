package com.example.geofencing.Services;

import java.util.List;
import java.util.UUID;

import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.geofencing.Components.GeometryUtils;
import com.example.geofencing.DTO.GeofenceDto;
import com.example.geofencing.Entities.Geofence;
import com.example.geofencing.Entities.User;
import com.example.geofencing.Enums.GeofenceType;
import com.example.geofencing.Exceptions.GeofencingException;
import com.example.geofencing.Payloads.CreateGeofenceRequest;
import com.example.geofencing.Reponses.PagedResponse;
import com.example.geofencing.Repositories.GeofenceRepository;
import com.example.geofencing.Repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
@Service
@RequiredArgsConstructor
@Slf4j
public class GeofenceService {
 
    private final GeofenceRepository geofenceRepository;
    private final UserRepository userRepository;
    private final CacheService cacheService;
 
    @Value("${geofencing.cache.geofence-ttl-seconds:600}") private long geofenceCacheTtl;
 
    @Transactional
    public GeofenceDto createGeofence(UUID ownerId, CreateGeofenceRequest request) {
        var owner = findUser(ownerId);
        validateGeofenceRequest(request);
 
        Geometry geometry = GeometryUtils.createGeofenceGeometry(
            request.getGeofenceType(),
            request.getCoordinates(),
            request.getCenterLat(), request.getCenterLng(),
            request.getRadiusMeters()
        );
 
        var geofence = Geofence.builder()
            .owner(owner)
            .name(request.getName())
            .description(request.getDescription())
            .geofenceType(request.getGeofenceType())
            .geometry(geometry)
            .radiusMeters(request.getRadiusMeters())
            .color(request.getColor() != null ? request.getColor() : "#FF5733")
            .enterAlert(request.getEnterAlert() != null ? request.getEnterAlert() : true)
            .exitAlert(request.getExitAlert() != null ? request.getExitAlert() : true)
            .dwellAlert(request.getDwellAlert() != null ? request.getDwellAlert() : false)
            .dwellTimeSeconds(request.getDwellTimeSeconds() != null ? request.getDwellTimeSeconds() : 300)
            .build();
 
        if (request.getGeofenceType() == GeofenceType.CIRCLE && request.getCenterLat() != null) {
            geofence.setCenterPoint(GeometryUtils.createPoint(request.getCenterLat(), request.getCenterLng()));
        }
 
        if (request.getMetadata() != null) {
            geofence.setMetadata(request.getMetadata());
        }
 
        geofence = geofenceRepository.save(geofence);
        log.info("Geofence created: {} by user {}", geofence.getId(), ownerId);
        return toDto(geofence);
    }
 
    @Transactional
    public GeofenceDto updateGeofence(UUID geofenceId, UUID userId, CreateGeofenceRequest request) {
        var geofence = findGeofenceAndVerifyOwner(geofenceId, userId);
 
        if (request.getName() != null) geofence.setName(request.getName());
        if (request.getDescription() != null) geofence.setDescription(request.getDescription());
        if (request.getColor() != null) geofence.setColor(request.getColor());
        if (request.getEnterAlert() != null) geofence.setEnterAlert(request.getEnterAlert());
        if (request.getExitAlert() != null) geofence.setExitAlert(request.getExitAlert());
        if (request.getDwellAlert() != null) geofence.setDwellAlert(request.getDwellAlert());
        if (request.getDwellTimeSeconds() != null) geofence.setDwellTimeSeconds(request.getDwellTimeSeconds());
 
        geofence = geofenceRepository.save(geofence);
        cacheService.invalidateGeofence(geofenceId.toString());
        return toDto(geofence);
    }
 
    @Transactional
    public void deleteGeofence(UUID geofenceId, UUID userId) {
        findGeofenceAndVerifyOwner(geofenceId, userId);
        geofenceRepository.deleteById(geofenceId);
        cacheService.invalidateGeofence(geofenceId.toString());
        log.info("Geofence {} deleted by user {}", geofenceId, userId);
    }
 
    @Transactional
    public void toggleGeofence(UUID geofenceId, UUID userId, boolean active) {
        var geofence = findGeofenceAndVerifyOwner(geofenceId, userId);
        geofence.setIsActive(active);
        geofenceRepository.save(geofence);
        cacheService.invalidateGeofence(geofenceId.toString());
    }
 
    public GeofenceDto getGeofence(UUID geofenceId) {
        return toDto(findGeofence(geofenceId));
    }
 
    public PagedResponse<GeofenceDto> getMyGeofences(UUID userId, int page, int size, boolean activeOnly) {
        var pageable = PageRequest.of(page, size);
        var result = activeOnly
            ? geofenceRepository.findByOwnerIdAndIsActive(userId, true, pageable)
            : geofenceRepository.findByOwnerId(userId, pageable);
 
        return PagedResponse.<GeofenceDto>builder()
            .content(result.getContent().stream().map(this::toDto).toList())
            .page(page).size(size)
            .totalElements(result.getTotalElements())
            .totalPages(result.getTotalPages())
            .hasNext(result.hasNext())
            .hasPrevious(result.hasPrevious())
            .build();
    }
 
    public List<GeofenceDto> getContainingGeofences(double lat, double lng, UUID userId) {
        return geofenceRepository.findContainingPointForUser(lat, lng, userId)
            .stream().map(this::toDto).toList();
    }
 
    public List<GeofenceDto> getNearbyGeofences(double lat, double lng, UUID userId, int limit) {
        return geofenceRepository.findNearestByOwner(lat, lng, userId, limit)
            .stream().map(this::toDto).toList();
    }
 
    // Called from location processing — optimized path
    public List<Geofence> getActiveGeofencesContainingPoint(double lat, double lng, UUID userId) {
        return geofenceRepository.findContainingPointForUser(lat, lng, userId);
    }
 
    public List<Geofence> getAllActiveGeofencesContainingPoint(double lat, double lng) {
        return geofenceRepository.findContainingPoint(lat, lng);
    }
 
    private Geofence findGeofence(UUID geofenceId) {
        return geofenceRepository.findById(geofenceId)
            .orElseThrow(() -> GeofencingException.notFound("Geofence"));
    }
 
    private Geofence findGeofenceAndVerifyOwner(UUID geofenceId, UUID userId) {
        var geofence = findGeofence(geofenceId);
        if (!geofence.getOwner().getId().equals(userId)) {
            throw GeofencingException.forbidden("You don't own this geofence");
        }
        return geofence;
    }
 
    private User findUser(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> GeofencingException.notFound("User"));
    }
 
    private void validateGeofenceRequest(CreateGeofenceRequest request) {
        switch (request.getGeofenceType()) {
            case CIRCLE -> {
                if (request.getCenterLat() == null || request.getCenterLng() == null
                    || request.getRadiusMeters() == null) {
                    throw GeofencingException.badRequest("Circle geofence requires centerLat, centerLng, and radiusMeters");
                }
            }
            case POLYGON, RECTANGLE -> {
                if (request.getCoordinates() == null || request.getCoordinates().size() < 3) {
                    throw GeofencingException.badRequest("Polygon geofence requires at least 3 coordinates");
                }
            }
        }
    }
 
    public GeofenceDto toDto(Geofence g) {
        var dto = GeofenceDto.builder()
            .id(g.getId())
            .name(g.getName())
            .description(g.getDescription())
            .ownerId(g.getOwner().getId())
            .geofenceType(g.getGeofenceType())
            .isActive(g.getIsActive())
            .enterAlert(g.getEnterAlert())
            .exitAlert(g.getExitAlert())
            .dwellAlert(g.getDwellAlert())
            .dwellTimeSeconds(g.getDwellTimeSeconds())
            .color(g.getColor())
            .metadata(g.getMetadata())
            .createdAt(g.getCreatedAt())
            .updatedAt(g.getUpdatedAt())
            .build();
 
        if (g.getGeofenceType() == GeofenceType.CIRCLE && g.getCenterPoint() != null) {
            dto.setCenterLat(g.getCenterPoint().getY());
            dto.setCenterLng(g.getCenterPoint().getX());
            dto.setRadiusMeters(g.getRadiusMeters());
        } else if (g.getGeometry() != null) {
            dto.setCoordinates(GeometryUtils.geometryToCoordinates(g.getGeometry()));
        }
        return dto;
    }
}