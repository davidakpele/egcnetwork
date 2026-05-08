package com.example.geofencing.Payloads;

import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data 
@NoArgsConstructor
@AllArgsConstructor 
@Builder
public class LocationUpdateRequest {
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
    private Double latitude;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double longitude;
    private Double altitude;
    private Double accuracy;
    private Double speed;
    private Double heading;
    @Min(0) @Max(100)
    private Integer batteryLevel;
    private String deviceId;
    private String provider;
    private LocalDateTime timestamp;
}
 