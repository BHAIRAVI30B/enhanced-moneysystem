package com.example.backend.exceptions;

public class InvalidRedemptionException extends RuntimeException {
    public InvalidRedemptionException(String message) {
        super(message);
    }
}
