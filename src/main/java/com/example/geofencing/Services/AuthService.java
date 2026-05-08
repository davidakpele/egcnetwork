package com.example.geofencing.Services;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.geofencing.Entities.RefreshToken;
import com.example.geofencing.Entities.User;
import com.example.geofencing.Enums.UserRole;
import com.example.geofencing.Enums.UserStatus;
import com.example.geofencing.Exceptions.GeofencingException;
import com.example.geofencing.Payloads.ChangePasswordRequest;
import com.example.geofencing.Payloads.LoginRequest;
import com.example.geofencing.Payloads.RegisterRequest;
import com.example.geofencing.Reponses.AuthResponse;
import com.example.geofencing.Repositories.RefreshTokenRepository;
import com.example.geofencing.Repositories.UserRepository;
import com.example.geofencing.Security.JwtTokenProvider;
import com.example.geofencing.Security.UserDetailsImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
 
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final DeviceService deviceService;
 
    @Value("${jwt.refresh-expiration}") private long refreshExpiration;
 
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw GeofencingException.conflict("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw GeofencingException.conflict("Email already registered");
        }
 
        var user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phone(request.getPhone())
            .role(UserRole.USER)
            .status(UserStatus.ACTIVE) // auto-activate for now; add email verification flow as needed
            .emailVerified(true)
            .emailVerificationToken(UUID.randomUUID().toString())
            .build();
 
        user = userRepository.save(user);
        log.info("New user registered: {}", user.getUsername());
 
        return buildAuthResponse(user, null, null);
    }
 
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword())
        );
 
        var userDetails = (UserDetailsImpl) auth.getPrincipal();
        var user = userRepository.findById(userDetails.getId())
            .orElseThrow(() -> GeofencingException.notFound("User"));
 
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw GeofencingException.unauthorized("Account is suspended");
        }
 
        userRepository.updateLastLogin(user.getId(), LocalDateTime.now());
 
        // Register/update device session
        if (request.getDeviceId() != null) {
            deviceService.registerDevice(user, request.getDeviceId(),
                request.getDeviceName(), request.getDeviceType());
        }
 
        // Revoke old refresh tokens
        refreshTokenRepository.revokeAllByUserId(user.getId());
 
        return buildAuthResponse(user, request.getDeviceId(), ipAddress);
    }
 
    @Transactional
    public AuthResponse refreshToken(String refreshTokenStr) {
        var storedToken = refreshTokenRepository.findByToken(refreshTokenStr)
            .orElseThrow(() -> GeofencingException.unauthorized("Invalid refresh token"));
 
        if (storedToken.isExpired() || storedToken.getRevoked()) {
            throw GeofencingException.unauthorized("Refresh token expired or revoked");
        }
 
        var user = storedToken.getUser();
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);
 
        return buildAuthResponse(user, null, null);
    }
 
    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("User {} logged out", userId);
    }
 
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        var user = userRepository.findById(userId)
            .orElseThrow(() -> GeofencingException.notFound("User"));
 
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw GeofencingException.badRequest("Current password is incorrect");
        }
 
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("Password changed for user {}", userId);
    }
 
    private AuthResponse buildAuthResponse(User user, String deviceId, String ipAddress) {
        String accessToken = jwtTokenProvider.generateAccessToken(
            user.getId(), user.getUsername(), user.getRole().name());
        String refreshTokenStr = jwtTokenProvider.generateRefreshToken(user.getId());
 
        var refreshToken = RefreshToken.builder()
            .user(user)
            .token(refreshTokenStr)
            .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
            .deviceInfo(deviceId)
            .ipAddress(ipAddress)
            .build();
        refreshTokenRepository.save(refreshToken);
 
        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshTokenStr)
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getExpiration() / 1000)
            .userId(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .role(user.getRole().name())
            .issuedAt(LocalDateTime.now())
            .build();
    }
}