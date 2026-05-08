package com.example.geofencing.Services;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.geofencing.DTO.AlertDto;
import com.example.geofencing.Entities.Alert;
import com.example.geofencing.Entities.Geofence;
import com.example.geofencing.Entities.GeofenceEvent;
import com.example.geofencing.Entities.User;
import com.example.geofencing.Enums.AlertSeverity;
import com.example.geofencing.Enums.AlertType;
import com.example.geofencing.Exceptions.GeofencingException;
import com.example.geofencing.Reponses.PagedResponse;
import com.example.geofencing.Repositories.AlertRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {
 
    private final AlertRepository alertRepository;
 
    @Transactional
    public Alert createAlert(User user, Geofence geofence, GeofenceEvent geofenceEvent,
                              AlertType type, AlertSeverity severity,
                              String title, String message, Map<String, Object> metadata) {
        var alert = Alert.builder()
            .user(user)
            .geofence(geofence)
            .geofenceEvent(geofenceEvent)
            .alertType(type)
            .severity(severity)
            .title(title)
            .message(message)
            .metadata(metadata)
            .build();
        alert = alertRepository.save(alert);
        log.debug("Alert created for user {}: {}", user.getId(), title);
        return alert;
    }
 
    public PagedResponse<AlertDto> getAlerts(UUID userId, boolean unreadOnly, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var result = unreadOnly
            ? alertRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false, pageable)
            : alertRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
 
        alertRepository.countByUserIdAndIsRead(userId, false);
 
        return PagedResponse.<AlertDto>builder()
            .content(result.getContent().stream().map(this::toDto).toList())
            .page(page).size(size)
            .totalElements(result.getTotalElements())
            .totalPages(result.getTotalPages())
            .hasNext(result.hasNext())
            .hasPrevious(result.hasPrevious())
            .build();
    }
 
    public long getUnreadCount(UUID userId) {
        return alertRepository.countByUserIdAndIsRead(userId, false);
    }
 
    @Transactional
    public AlertDto markRead(UUID alertId, UUID userId) {
        var alert = findAlertAndVerifyOwner(alertId, userId);
        alert.setIsRead(true);
        return toDto(alertRepository.save(alert));
    }
 
    @Transactional
    public AlertDto acknowledge(UUID alertId, UUID userId) {
        var alert = findAlertAndVerifyOwner(alertId, userId);
        alert.setIsAcknowledged(true);
        alert.setAcknowledgedAt(LocalDateTime.now());
        alert.setIsRead(true);
        return toDto(alertRepository.save(alert));
    }
 
    @Transactional
    public int markAllRead(UUID userId) {
        return alertRepository.markAllReadByUser(userId);
    }
 
    private Alert findAlertAndVerifyOwner(UUID alertId, UUID userId) {
        var alert = alertRepository.findById(alertId)
            .orElseThrow(() -> GeofencingException.notFound("Alert"));
        if (!alert.getUser().getId().equals(userId)) {
            throw GeofencingException.forbidden("Access denied to this alert");
        }
        return alert;
    }
 
    public AlertDto toDto(Alert a) {
        return AlertDto.builder()
            .id(a.getId())
            .userId(a.getUser().getId())
            .geofenceId(a.getGeofence() != null ? a.getGeofence().getId() : null)
            .alertType(a.getAlertType())
            .severity(a.getSeverity())
            .title(a.getTitle())
            .message(a.getMessage())
            .isRead(a.getIsRead())
            .isAcknowledged(a.getIsAcknowledged())
            .acknowledgedAt(a.getAcknowledgedAt())
            .createdAt(a.getCreatedAt())
            .build();
    }
}