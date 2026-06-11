package com.example.backend.exceptions;

public class DuplicateTransferException extends RuntimeException {
    public DuplicateTransferException(String message) {
        super(message);
    }
}
