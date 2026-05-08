package com.example.geofencing.Components;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.geofencing.Security.UserDetailsImpl;
 
public class SecurityUtils {
 
    public static UserDetailsImpl getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl ud) {
            return ud;
        }
        throw new IllegalStateException("No authenticated user found");
    }
 
    public static UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }
}