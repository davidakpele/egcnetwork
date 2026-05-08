package com.example.geofencing.Services;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.geofencing.Entities.DeviceSession;
import com.example.geofencing.Entities.User;
import com.example.geofencing.Repositories.DeviceSessionRepository;

import lombok.RequiredArgsConstructor;
 
@Service
@RequiredArgsConstructor
public class DeviceService {
 
    private final DeviceSessionRepository deviceSessionRepository;
 
    @Transactional
    public DeviceSession registerDevice(User user, String deviceId, String deviceName, String deviceType) {
        return deviceSessionRepository.findByUserIdAndDeviceId(user.getId(), deviceId)
            .map(existing -> {
                existing.setDeviceName(deviceName);
                existing.setDeviceType(deviceType);
                existing.setLastSeen(LocalDateTime.now());
                existing.setIsActive(true);
                return deviceSessionRepository.save(existing);
            })
            .orElseGet(() -> deviceSessionRepository.save(
                DeviceSession.builder()
                    .user(user)
                    .deviceId(deviceId)
                    .deviceName(deviceName)
                    .deviceType(deviceType)
                    .lastSeen(LocalDateTime.now())
                    .build()
            ));
    }
 
    @Transactional
    public void updateLastSeen(User user, String deviceId) {
        deviceSessionRepository.findByUserIdAndDeviceId(user.getId(), deviceId)
            .ifPresent(d -> {
                d.setLastSeen(LocalDateTime.now());
                deviceSessionRepository.save(d);
            });
    }
}