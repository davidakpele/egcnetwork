package com.example.geofencing.Services;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {
 
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper redisObjectMapper;
 
    private static final String USER_LOCATION_PREFIX = "user:location:";
    private static final String USER_GEOFENCES_PREFIX = "user:geofences:";
    private static final String GEOFENCE_PREFIX = "geofence:";
    private static final String USER_SESSION_PREFIX = "user:session:";
    private static final String TRACKING_PREFIX = "tracking:";
    private static final String GEOFENCE_STATE_PREFIX = "geofence:state:"; 
 
    // ---- Location Cache ----
    public void cacheUserLocation(String userId, Object location, long ttlSeconds) {
        redisTemplate.opsForValue().set(USER_LOCATION_PREFIX + userId, location, ttlSeconds, TimeUnit.SECONDS);
    }
 
    public <T> Optional<T> getUserLocation(String userId, Class<T> clazz) {
        return getAs(USER_LOCATION_PREFIX + userId, clazz);
    }
 
    // ---- Geofence State (is user inside geofence) ----
    public void setUserGeofenceState(String userId, String geofenceId, boolean inside, long ttlSeconds) {
        String key = GEOFENCE_STATE_PREFIX + userId + ":" + geofenceId;
        redisTemplate.opsForValue().set(key, inside, ttlSeconds, TimeUnit.SECONDS);
    }
 
    public Optional<Boolean> getUserGeofenceState(String userId, String geofenceId) {
        String key = GEOFENCE_STATE_PREFIX + userId + ":" + geofenceId;
        Object val = redisTemplate.opsForValue().get(key);
        if (val instanceof Boolean b) return Optional.of(b);
        if (val != null) return Optional.of(Boolean.parseBoolean(val.toString()));
        return Optional.empty();
    }
 
    // ---- Geofence Cache ----
    public void cacheGeofence(String geofenceId, Object geofence, long ttlSeconds) {
        redisTemplate.opsForValue().set(GEOFENCE_PREFIX + geofenceId, geofence, ttlSeconds, TimeUnit.SECONDS);
    }
 
    public <T> Optional<T> getGeofence(String geofenceId, Class<T> clazz) {
        return getAs(GEOFENCE_PREFIX + geofenceId, clazz);
    }
 
    public void invalidateGeofence(String geofenceId) {
        redisTemplate.delete(GEOFENCE_PREFIX + geofenceId);
    }
 
    // ---- Active Geofences for User ----
    public void addToActiveGeofences(String userId, String geofenceId) {
        redisTemplate.opsForSet().add(USER_GEOFENCES_PREFIX + userId, geofenceId);
        redisTemplate.expire(USER_GEOFENCES_PREFIX + userId, Duration.ofHours(24));
    }
 
    public void removeFromActiveGeofences(String userId, String geofenceId) {
        redisTemplate.opsForSet().remove(USER_GEOFENCES_PREFIX + userId, geofenceId);
    }
 
    public Set<Object> getActiveGeofencesForUser(String userId) {
        Set<Object> members = redisTemplate.opsForSet().members(USER_GEOFENCES_PREFIX + userId);
        return members != null ? members : Collections.emptySet();
    }
 
    // ---- Dwell Time Tracking ----
    public void startDwellTimer(String userId, String geofenceId) {
        String key = "dwell:" + userId + ":" + geofenceId;
        redisTemplate.opsForValue().set(key, System.currentTimeMillis(), 24, TimeUnit.HOURS);
    }
 
    public Optional<Long> getDwellStartTime(String userId, String geofenceId) {
        String key = "dwell:" + userId + ":" + geofenceId;
        Object val = redisTemplate.opsForValue().get(key);
        if (val instanceof Long l) return Optional.of(l);
        if (val instanceof Integer i) return Optional.of(i.longValue());
        if (val != null) {
            try { return Optional.of(Long.parseLong(val.toString())); } catch (NumberFormatException ignored) {}
        }
        return Optional.empty();
    }
 
    public void clearDwellTimer(String userId, String geofenceId) {
        redisTemplate.delete("dwell:" + userId + ":" + geofenceId);
    }
 
    // ---- Location History in Redis (ring buffer) ----
    public void pushLocationHistory(String userId, Object location, long maxHistory) {
        String key = "location:history:" + userId;
        redisTemplate.opsForList().leftPush(key, location);
        redisTemplate.opsForList().trim(key, 0, maxHistory - 1);
        redisTemplate.expire(key, Duration.ofHours(24));
    }
 
    public List<Object> getRecentLocationHistory(String userId, int count) {
        String key = "location:history:" + userId;
        List<Object> result = redisTemplate.opsForList().range(key, 0, count - 1);
        return result != null ? result : Collections.emptyList();
    }
 
    // ---- Rate Limiting ----
    public boolean checkAndIncrementRateLimit(String userId, String action, int maxRequests, long windowSeconds) {
        String key = "rate:" + action + ":" + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        return count != null && count <= maxRequests;
    }
 
    // ---- Generic ----
    public void set(String key, Object value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
    }
 
    public Optional<Object> get(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }
 
    public void delete(String key) {
        redisTemplate.delete(key);
    }
 
    private <T> Optional<T> getAs(String key, Class<T> clazz) {
        Object val = redisTemplate.opsForValue().get(key);
        if (val == null) return Optional.empty();
        try {
            return Optional.of(redisObjectMapper.convertValue(val, clazz));
        } catch (Exception e) {
            log.warn("Failed to deserialize cache value for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }
}