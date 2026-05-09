package com.example.geofencing.Controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.geofencing.Components.SecurityUtils;
import com.example.geofencing.DTO.UserDto;
import com.example.geofencing.Payloads.UpdateUserRequest;
import com.example.geofencing.Reponses.ApiResponse;
import com.example.geofencing.Services.UserService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserDto>> getMyProfile() {
        return ResponseEntity.ok(ApiResponse.success(
            userService.getUser(SecurityUtils.getCurrentUserId())));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserDto>> updateMyProfile(@Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            userService.updateUser(SecurityUtils.getCurrentUserId(), request), "Profile updated"));
    }

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount() {
        userService.deleteUser(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Account deleted"));
    }

    // Admin: get any user
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable java.util.UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUser(userId)));
    }
}