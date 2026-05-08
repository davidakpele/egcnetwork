package com.example.geofencing.Controllers;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.geofencing.Components.SecurityUtils;
import com.example.geofencing.DTO.GeofenceEventDto;
import com.example.geofencing.DTO.LocationDto;
import com.example.geofencing.Payloads.LocationUpdateRequest;
import com.example.geofencing.Reponses.ApiResponse;
import com.example.geofencing.Reponses.PagedResponse;
import com.example.geofencing.Services.LocationService;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    /** Update current user's location */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LocationDto>> updateLocation(
            @Valid @RequestBody LocationUpdateRequest request) {
        var result = locationService.processLocationUpdate(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(result, "Location updated"));
    }

    /** Get current user's latest location */
    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LocationDto>> getCurrentLocation() {
        var loc = locationService.getCurrentLocation(SecurityUtils.getCurrentUserId());
        return loc.map(l -> ResponseEntity.ok(ApiResponse.success(l)))
            .orElse(ResponseEntity.ok(ApiResponse.error("NOT_FOUND", "No location recorded yet")));
    }

    /** Get current user's location history */
    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PagedResponse<LocationDto>>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        var result = locationService.getLocationHistory(SecurityUtils.getCurrentUserId(), from, to, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /** Get geofence events for current user */
    @GetMapping("/events")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PagedResponse<GeofenceEventDto>>> getGeofenceEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = locationService.getGeofenceEvents(SecurityUtils.getCurrentUserId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /** Start a tracking session */
    @PostMapping("/tracking/start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> startTracking(
            @RequestParam(required = false) String deviceId) {
        locationService.startTracking(SecurityUtils.getCurrentUserId(), deviceId);
        return ResponseEntity.ok(ApiResponse.success(null, "Tracking started"));
    }

    /** Stop current tracking session */
    @PostMapping("/tracking/stop")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> stopTracking() {
        locationService.stopTracking(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Tracking stopped"));
    }

    /** Admin: get any user's current location */
    @GetMapping("/{userId}/current")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LocationDto>> getUserCurrentLocation(@PathVariable UUID userId) {
        var loc = locationService.getCurrentLocation(userId);
        return loc.map(l -> ResponseEntity.ok(ApiResponse.success(l)))
            .orElse(ResponseEntity.ok(ApiResponse.error("NOT_FOUND", "No location recorded")));
    }

    /** Admin: get any user's location history */
    @GetMapping("/{userId}/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<LocationDto>>> getUserHistory(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var result = locationService.getLocationHistory(userId, null, null, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}