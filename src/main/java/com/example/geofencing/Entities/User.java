package com.example.geofencing.Entities;


import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import com.example.geofencing.Enums.UserRole;
import com.example.geofencing.Enums.UserStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    @Column(unique = true, nullable = false, length = 50)
    private String username;
 
    @Column(unique = true, nullable = false, length = 100)
    private String email;
 
    @Column(nullable = false)
    private String password;
 
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;
 
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;
 
    @Column(length = 20)
    private String phone;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING_VERIFICATION;
 
    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false;
 
    @Column(name = "email_verification_token")
    private String emailVerificationToken;
 
    @Column(name = "password_reset_token")
    private String passwordResetToken;
 
    @Column(name = "password_reset_expires")
    private LocalDateTime passwordResetExpires;
 
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
 
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
 
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}