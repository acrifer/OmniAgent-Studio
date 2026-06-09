package com.devmind.studio.controller;

import com.devmind.studio.dto.ApiResponse;
import com.devmind.studio.dto.AuthDtos.LoginRequest;
import com.devmind.studio.dto.AuthDtos.RegisterRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody RegisterRequest request) {
        return ApiResponse.fail("演示模式已停用账号注册，请直接进入工作台。");
    }

    @PostMapping("/login")
    public ApiResponse<Void> login(@RequestBody LoginRequest request) {
        return ApiResponse.fail("演示模式已停用账号登录，请直接进入工作台。");
    }
}
