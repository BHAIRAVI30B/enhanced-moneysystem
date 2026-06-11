package com.example.backend.security.payload.response;

import java.util.List;

public class JwtResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private Long id;
    private String username;
    private List<String> roles;

    // Default constructor
    public JwtResponse() {
    }

    // All-args constructor
    public JwtResponse(String accessToken, String tokenType, Long id, String username, List<String> roles) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.id = id;
        this.username = username;
        this.roles = roles;
    }

    // Custom constructor (without tokenType, defaults to "Bearer")
    public JwtResponse(String accessToken, Long id, String username, List<String> roles) {
        this.accessToken = accessToken;
        this.id = id;
        this.username = username;
        this.roles = roles;
    }

    // Getters
    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getRoles() {
        return roles;
    }

    // Setters
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    // Convenience method
    public String getToken() {
        return accessToken;
    }

    // toString method
    @Override
    public String toString() {
        return "JwtResponse{" +
                "accessToken='" + accessToken + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", id=" + id +
                ", username='" + username + '\'' +
                ", roles=" + roles +
                '}';
    }
}
