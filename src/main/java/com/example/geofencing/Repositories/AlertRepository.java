package com.example.geofencing.Repositories;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.geofencing.Entities.Alert;
 
@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {
 
    Page<Alert> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
 
    Page<Alert> findByUserIdAndIsReadOrderByCreatedAtDesc(UUID userId, Boolean isRead, Pageable pageable);
 
    long countByUserIdAndIsRead(UUID userId, Boolean isRead);
 
    @Modifying
    @Query("UPDATE Alert a SET a.isRead = true WHERE a.user.id = :userId AND a.isRead = false")
    int markAllReadByUser(@Param("userId") UUID userId);
}