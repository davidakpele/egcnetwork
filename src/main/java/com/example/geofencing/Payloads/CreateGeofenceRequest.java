package com.example.geofencing.Payloads;

import java.util.List;
import java.util.Map;

import com.example.geofencing.Enums.GeofenceType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateGeofenceRequest {
    @NotBlank @Size(max=100) private String name;
    private String description;
    @NotNull private GeofenceType geofenceType;
    private List<CoordinateDto> coordinates; // for POLYGON/RECTANGLE
    private Double centerLat;   // for CIRCLE
    private Double centerLng;
    private Double radiusMeters;
    private Boolean enterAlert;
    private Boolean exitAlert;
    private Boolean dwellAlert;
    private Integer dwellTimeSeconds;
    private String color;
    private Map<String, Object> metadata;
 
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CoordinateDto {
        @DecimalMin("-90.0") @DecimalMax("90.0") private Double latitude;
        @DecimalMin("-180.0") @DecimalMax("180.0") private Double longitude;
    }
}