package com.example.geofencing.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.geofencing.Enums.GeofenceEventType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GeofenceEventDto {
    private UUID id;
    private UUID userId;
    private UUID geofenceId;
    private String geofenceName;
    private GeofenceEventType eventType;
    private Double latitude;
    private Double longitude;
    private Integer dwellDurationSeconds;
    private LocalDateTime occurredAt;
}
 