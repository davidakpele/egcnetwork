package com.example.geofencing.Controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.geofencing.Components.SecurityUtils;
import com.example.geofencing.Payloads.ChangePasswordRequest;
import com.example.geofencing.Payloads.LoginRequest;
import com.example.geofencing.Payloads.RegisterRequest;
import com.example.geofencing.Reponses.ApiResponse;
import com.example.geofencing.Reponses.AuthResponse;
import com.example.geofencing.Services.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        var result = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(result, "Registration successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        var result = authService.login(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(result, "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestParam String refreshToken) {
        var result = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(result, "Token refreshed"));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout() {
        authService.logout(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }
}