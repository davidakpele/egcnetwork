package com.example.geofencing.Services;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.geofencing.Repositories.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskService {
 
    private final RefreshTokenRepository refreshTokenRepository;
 
    @Scheduled(cron = "0 0 2 * * *") // daily at 2 AM
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredAndRevoked(LocalDateTime.now());
        log.info("Expired refresh tokens cleaned up");
    }
}