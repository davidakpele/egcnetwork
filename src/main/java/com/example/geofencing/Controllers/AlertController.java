package com.example.geofencing.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.geofencing.Components.SecurityUtils;
import com.example.geofencing.DTO.AlertDto;
import com.example.geofencing.Reponses.ApiResponse;
import com.example.geofencing.Reponses.PagedResponse;
import com.example.geofencing.Services.AlertService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PagedResponse<AlertDto>>> getAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        var result = alertService.getAlerts(SecurityUtils.getCurrentUserId(), unreadOnly, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        long count = alertService.getUnreadCount(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count)));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AlertDto>> markRead(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
            alertService.markRead(id, SecurityUtils.getCurrentUserId()), "Alert marked as read"));
    }

    @PatchMapping("/{id}/acknowledge")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AlertDto>> acknowledge(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
            alertService.acknowledge(id, SecurityUtils.getCurrentUserId()), "Alert acknowledged"));
    }

    @PostMapping("/mark-all-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllRead() {
        int count = alertService.markAllRead(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("markedRead", count), "All alerts marked as read"));
    }
}