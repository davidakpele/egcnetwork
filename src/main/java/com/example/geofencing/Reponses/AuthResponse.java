package com.example.geofencing.Reponses;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private UUID userId;
    private String username;
    private String email;
    private String role;
    private LocalDateTime issuedAt;
}
 