package com.example.geofencing.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.example.geofencing.DTO.KafkaGeofenceEvent;
import com.example.geofencing.DTO.KafkaLocationEvent;

import java.util.concurrent.CompletableFuture;
 
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {
 
    private final KafkaTemplate<String, Object> kafkaTemplate;
 
    @Value("${geofencing.kafka.topics.location-updates}") private String locationUpdatesTopic;
    @Value("${geofencing.kafka.topics.geofence-events}") private String geofenceEventsTopic;
    @Value("${geofencing.kafka.topics.alerts}") private String alertsTopic;
 
    public void publishLocationUpdate(KafkaLocationEvent event) {
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(locationUpdatesTopic, event.getUserId().toString(), event);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish location update for user {}: {}", event.getUserId(), ex.getMessage());
            } else {
                log.debug("Location update published for user {} at offset {}",
                    event.getUserId(), result.getRecordMetadata().offset());
            }
        });
    }
 
    public void publishGeofenceEvent(KafkaGeofenceEvent event) {
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(geofenceEventsTopic, event.getUserId().toString(), event);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish geofence event for user {}: {}", event.getUserId(), ex.getMessage());
            } else {
                log.info("Geofence event published: user={} geofence={} type={}",
                    event.getUserId(), event.getGeofenceId(), event.getEventType());
            }
        });
    }
 
    public void publishAlert(Object alert, String userId) {
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(alertsTopic, userId, alert);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish alert for user {}: {}", userId, ex.getMessage());
            }
        });
    }
}