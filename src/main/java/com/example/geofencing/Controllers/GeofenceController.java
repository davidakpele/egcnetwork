package com.example.geofencing.Controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.geofencing.Components.SecurityUtils;
import com.example.geofencing.DTO.GeofenceDto;
import com.example.geofencing.Payloads.CreateGeofenceRequest;
import com.example.geofencing.Reponses.ApiResponse;
import com.example.geofencing.Reponses.PagedResponse;
import com.example.geofencing.Services.GeofenceService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/geofences")
@RequiredArgsConstructor
public class GeofenceController {

    private final GeofenceService geofenceService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<GeofenceDto>> create(@Valid @RequestBody CreateGeofenceRequest request) {
        var result = geofenceService.createGeofence(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result, "Geofence created"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<GeofenceDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateGeofenceRequest request) {
        var result = geofenceService.updateGeofence(id, SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(result, "Geofence updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        geofenceService.deleteGeofence(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Geofence deleted"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<GeofenceDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(geofenceService.getGeofence(id)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PagedResponse<GeofenceDto>>> listMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        var result = geofenceService.getMyGeofences(SecurityUtils.getCurrentUserId(), page, size, activeOnly);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable UUID id) {
        geofenceService.toggleGeofence(id, SecurityUtils.getCurrentUserId(), true);
        return ResponseEntity.ok(ApiResponse.success(null, "Geofence activated"));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        geofenceService.toggleGeofence(id, SecurityUtils.getCurrentUserId(), false);
        return ResponseEntity.ok(ApiResponse.success(null, "Geofence deactivated"));
    }

    /** Check which of the user's geofences contain a given point */
    @GetMapping("/contains")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<GeofenceDto>>> containsPoint(
            @RequestParam double lat,
            @RequestParam double lng) {
        var result = geofenceService.getContainingGeofences(lat, lng, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /** Find nearby geofences */
    @GetMapping("/nearby")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<GeofenceDto>>> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") int limit) {
        var result = geofenceService.getNearbyGeofences(lat, lng, SecurityUtils.getCurrentUserId(), limit);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}