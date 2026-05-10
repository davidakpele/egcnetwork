package com.example.geofencing.Interfaces;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.security.core.userdetails.UserDetails;

import io.jsonwebtoken.Claims;

public interface IJwtService {
    String extractUsername(String token);

    Long extractUserId(String token);

    List<String> extractRoles(String token);

    <T> T extractClaim(String token, Function<Claims, T> claimsResolver);

    Claims extractAllClaims(String token);

    String generateToken(UserDetails userDetails, UUID userId);

    String generateRefreshToken(UserDetails userDetails);

    boolean isTokenValid(String token, UserDetails userDetails);

    boolean validateToken(String token);

    boolean validateTokenClaims(String token);

    Date getExpirationDate(String token);

    boolean isTokenExpired(String token);

    long getRemainingValidity(String token);

    String getTokenType(String token);
}
