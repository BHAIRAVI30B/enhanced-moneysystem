package com.example.backend.security.payload.response;

public record SignupResponse(Long userId, String username, String accountId, String holderName, Double balance) {

}
