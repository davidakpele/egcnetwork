package com.example.geofencing.Payloads;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data 
@NoArgsConstructor 
@AllArgsConstructor
public class UpdateUserRequest {
    @Size(max=50) private String firstName;
    @Size(max=50) private String lastName;
    private String phone;
}
 