package com.devmind.studio.service;

import com.devmind.studio.dto.AuthDtos.AuthResponse;
import com.devmind.studio.dto.AuthDtos.LoginRequest;
import com.devmind.studio.dto.AuthDtos.RegisterRequest;
import com.devmind.studio.entity.User;
import com.devmind.studio.repository.UserRepository;
import com.devmind.studio.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwtService) {
        this.users = users;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (users.existsByUsername(request.username())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(encoder.encode(request.password()));
        users.save(user);
        return toAuth(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = users.findByUsername(request.username()).orElseThrow(() -> new IllegalArgumentException("账号或密码错误"));
        if (!encoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        return toAuth(user);
    }

    private AuthResponse toAuth(User user) {
        return new AuthResponse(jwtService.generateToken(user.getId(), user.getUsername()), user.getId(), user.getUsername());
    }
}
