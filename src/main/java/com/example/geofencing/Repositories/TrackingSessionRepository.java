package com.example.geofencing.Repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.geofencing.Entities.TrackingSession;
import com.example.geofencing.Enums.TrackingStatus;
 
@Repository
public interface TrackingSessionRepository extends JpaRepository<TrackingSession, UUID> {
    Optional<TrackingSession> findTopByUserIdAndStatusOrderByStartedAtDesc(UUID userId, TrackingStatus status);
 
    @Modifying
    @Query("UPDATE TrackingSession s SET s.updateCount = s.updateCount + 1, s.totalDistanceMeters = s.totalDistanceMeters + :dist WHERE s.id = :id")
    void incrementStats(@Param("id") UUID id, @Param("dist") double dist);
}
 