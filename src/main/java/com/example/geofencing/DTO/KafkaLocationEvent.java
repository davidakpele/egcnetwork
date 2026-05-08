package com.example.geofencing.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class KafkaLocationEvent {
    private String eventId;
    private UUID userId;
    private Double latitude;
    private Double longitude;
    private Double altitude;
    private Double speed;
    private Double heading;
    private Integer batteryLevel;
    private String deviceId;
    private LocalDateTime timestamp;
    private String sessionId;
}
