package com.example.backend.exceptions;

public class UserAuthenticationException extends RuntimeException {
    public UserAuthenticationException(String message) {
        super(message);
    }
}

