package com.example.geofencing.Repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.geofencing.Entities.DeviceSession;
 
@Repository
public interface DeviceSessionRepository extends JpaRepository<DeviceSession, UUID> {
    Optional<DeviceSession> findByUserIdAndDeviceId(UUID userId, String deviceId);
}
 
