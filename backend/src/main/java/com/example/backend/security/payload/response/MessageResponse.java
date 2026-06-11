package com.example.backend.security.payload.response;

public class MessageResponse {

    private String message;

    // Default constructor
    public MessageResponse() {
    }

    // All-args constructor
    public MessageResponse(String message) {
        this.message = message;
    }

    // Getter
    public String getMessage() {
        return message;
    }

    // Setter
    public void setMessage(String message) {
        this.message = message;
    }

    // toString method
    @Override
    public String toString() {
        return "MessageResponse{" +
                "message='" + message + '\'' +
                '}';
    }
}
