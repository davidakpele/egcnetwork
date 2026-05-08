package com.example.geofencing.Payloads;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data 
@NoArgsConstructor 
@AllArgsConstructor
public class LoginRequest {
    @NotBlank private String usernameOrEmail;
    @NotBlank private String password;
    private String deviceId;
    private String deviceName;
    private String deviceType;
}