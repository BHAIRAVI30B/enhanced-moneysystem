package com.example.backend.security.payload.request;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    // Default constructor
    public LoginRequest() {
    }

    // All-args constructor
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    // Setters
    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // toString method
    @Override
    public String toString() {
        return "LoginRequest{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
