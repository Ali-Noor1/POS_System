package com.possystem.dto;

public class LoginResponse {

    private Long id;
    private String fullName;
    private String username;
    private String role;
    private String token;
    private String tokenType;
    private String message;

    public LoginResponse(
            Long id,
            String fullName,
            String username,
            String role,
            String token,
            String tokenType,
            String message
    ) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.role = role;
        this.token = token;
        this.tokenType = tokenType;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getMessage() {
        return message;
    }
}