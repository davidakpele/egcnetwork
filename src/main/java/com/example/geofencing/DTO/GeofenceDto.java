package com.example.geofencing.DTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.example.geofencing.Enums.GeofenceType;
import com.example.geofencing.Payloads.CreateGeofenceRequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GeofenceDto {
    private UUID id;
    private String name;
    private String description;
    private UUID ownerId;
    private GeofenceType geofenceType;
    private List<CreateGeofenceRequest.CoordinateDto> coordinates;
    private Double centerLat;
    private Double centerLng;
    private Double radiusMeters;
    private Boolean isActive;
    private Boolean enterAlert;
    private Boolean exitAlert;
    private Boolean dwellAlert;
    private Integer dwellTimeSeconds;
    private String color;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}