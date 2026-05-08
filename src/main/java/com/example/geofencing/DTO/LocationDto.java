package com.example.geofencing.DTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class LocationDto {
    private UUID id;
    private UUID userId;
    private Double latitude;
    private Double longitude;
    private Double altitude;
    private Double accuracy;
    private Double speed;
    private Double heading;
    private Integer batteryLevel;
    private String deviceId;
    private String provider;
    private LocalDateTime timestamp;
    private List<ActiveGeofenceDto> activeGeofences;
 
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ActiveGeofenceDto {
        private UUID geofenceId;
        private String name;
        private boolean inside;
        private double distanceToBoundaryMeters;
    }
}