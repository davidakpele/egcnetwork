package com.example.geofencing.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.geofencing.Enums.AlertSeverity;
import com.example.geofencing.Enums.AlertType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AlertDto {
    private UUID id;
    private UUID userId;
    private UUID geofenceId;
    private AlertType alertType;
    private AlertSeverity severity;
    private String title;
    private String message;
    private Boolean isRead;
    private Boolean isAcknowledged;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime createdAt;
}
 