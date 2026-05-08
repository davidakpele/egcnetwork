package com.example.geofencing.Exceptions;

import org.springframework.http.HttpStatus;

import lombok.Getter;
 
@Getter
public class GeofencingException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;
 
    public GeofencingException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
 
    public static GeofencingException notFound(String resource) {
        return new GeofencingException(resource + " not found", HttpStatus.NOT_FOUND, "NOT_FOUND");
    }
 
    public static GeofencingException unauthorized(String message) {
        return new GeofencingException(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
 
    public static GeofencingException forbidden(String message) {
        return new GeofencingException(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }
 
    public static GeofencingException conflict(String message) {
        return new GeofencingException(message, HttpStatus.CONFLICT, "CONFLICT");
    }
 
    public static GeofencingException badRequest(String message) {
        return new GeofencingException(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }
}