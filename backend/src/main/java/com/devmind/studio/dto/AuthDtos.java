package com.devmind.studio.dto;

public class AuthDtos {
    public record RegisterRequest(String username, String password, String email) {}
    public record LoginRequest(String username, String password) {}
    public record AuthResponse(String token, Long userId, String username) {}
}
