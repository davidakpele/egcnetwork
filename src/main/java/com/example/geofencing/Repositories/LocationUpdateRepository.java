package com.example.geofencing.Repositories;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.geofencing.Entities.LocationUpdate;
 
@Repository
public interface LocationUpdateRepository extends JpaRepository<LocationUpdate, UUID> {
 
    Optional<LocationUpdate> findTopByUserIdOrderByTimestampDesc(UUID userId);
 
    Page<LocationUpdate> findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);
 
    @Query("SELECT l FROM LocationUpdate l WHERE l.user.id = :userId AND l.timestamp BETWEEN :from AND :to ORDER BY l.timestamp DESC")
    Page<LocationUpdate> findByUserIdAndTimeRange(
        @Param("userId") UUID userId, @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to, Pageable pageable);
 
    @Query("SELECT COUNT(l) FROM LocationUpdate l WHERE l.user.id = :userId AND l.timestamp > :since")
    long countRecentByUser(@Param("userId") UUID userId, @Param("since") LocalDateTime since);
}