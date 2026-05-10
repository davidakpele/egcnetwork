package com.example.geofencing.Exceptions;

public class UserClientNotFoundException extends RuntimeException {
    private final String details;

    public UserClientNotFoundException(String message, String details) {
        super(message);
        this.details = details;
    }

    public String getDetails() {
        return details;
    }
}
