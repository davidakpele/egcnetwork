package com.example.geofencing.Components;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    @NotBlank(message = "JWT secret is required")
    private String secret;

    @NotNull(message = "Token expiration is required")
    @Positive(message = "Token expiration must be positive")
    @Min(value = 1, message = "Token expiration must be at least 1ms")
    private Long expiration = 86400000L;

    @NotNull(message = "Refresh token expiration is required")
    @Positive(message = "Refresh token expiration must be positive")
    @Min(value = 1, message = "Refresh token expiration must be at least 1ms")
    private Long refreshExpiration = 604800000L;

    private String issuer = "geofencing";
    private String audience = "geofencing-client";

    public int getExpirationMinutes() {
        return (int) (expiration / 1000 / 60);
    }

    public int getRefreshExpirationDays() {
        return (int) (refreshExpiration / 1000 / 60 / 60 / 24);
    }

    public long getExpirationMillis() {
        return expiration;
    }

    public long getRefreshExpirationMillis() {
        return refreshExpiration;
    }

    public String getSecretKey() {
        return secret;
    }
}