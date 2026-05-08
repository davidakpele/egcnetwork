package com.example.geofencing.Payloads;

import jakarta.validation.constraints.*;
import lombok.*;
 
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class RegisterRequest {
    @NotBlank @Size(min=3, max=50)
    private String username;
    @NotBlank @Email
    private String email;
    @NotBlank @Size(min=8, max=100)
    private String password;
    @NotBlank @Size(max=50)
    private String firstName;
    @NotBlank @Size(max=50)
    private String lastName;
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
    private String phone;
}