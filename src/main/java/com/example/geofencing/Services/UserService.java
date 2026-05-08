package com.example.geofencing.Services;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.geofencing.DTO.UserDto;
import com.example.geofencing.Entities.User;
import com.example.geofencing.Enums.UserStatus;

import java.util.UUID;

import com.example.geofencing.Exceptions.GeofencingException;
import com.example.geofencing.Payloads.UpdateUserRequest;
import com.example.geofencing.Repositories.UserRepository;
 
@Service
@RequiredArgsConstructor
public class UserService {
 
    private final UserRepository userRepository;
 
    public UserDto getUser(UUID userId) {
        return toDto(findUser(userId));
    }
 
    @Transactional
    public UserDto updateUser(UUID userId, UpdateUserRequest request) {
        var user = findUser(userId);
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        return toDto(userRepository.save(user));
    }
 
    @Transactional
    public void deleteUser(UUID userId) {
        var user = findUser(userId);
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }
 
    private User findUser(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> GeofencingException.notFound("User"));
    }
 
    public static UserDto toDto(User user) {
        return UserDto.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .phone(user.getPhone())
            .role(user.getRole().name())
            .status(user.getStatus().name())
            .emailVerified(user.getEmailVerified())
            .lastLogin(user.getLastLogin())
            .createdAt(user.getCreatedAt())
            .build();
    }
}