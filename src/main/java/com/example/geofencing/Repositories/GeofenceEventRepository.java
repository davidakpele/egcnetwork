package com.example.geofencing.Repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.geofencing.Entities.GeofenceEvent;
import com.example.geofencing.Enums.GeofenceEventType;
 
@Repository
public interface GeofenceEventRepository extends JpaRepository<GeofenceEvent, UUID> {
 
    Page<GeofenceEvent> findByUserIdOrderByOccurredAtDesc(UUID userId, Pageable pageable);
 
    Page<GeofenceEvent> findByGeofenceIdOrderByOccurredAtDesc(UUID geofenceId, Pageable pageable);
 
    Optional<GeofenceEvent> findTopByUserIdAndGeofenceIdOrderByOccurredAtDesc(UUID userId, UUID geofenceId);
 
    @Query("SELECT e FROM GeofenceEvent e WHERE e.user.id = :userId AND e.geofence.id = :geofenceId AND e.eventType = :type ORDER BY e.occurredAt DESC")
    List<GeofenceEvent> findRecentByUserAndGeofence(
        @Param("userId") UUID userId, @Param("geofenceId") UUID geofenceId,
        @Param("type") GeofenceEventType type, Pageable pageable);
 
    @Query("SELECT e FROM GeofenceEvent e WHERE e.user.id = :userId AND e.occurredAt BETWEEN :from AND :to ORDER BY e.occurredAt DESC")
    Page<GeofenceEvent> findByUserAndTimeRange(
        @Param("userId") UUID userId, @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to, Pageable pageable);
}