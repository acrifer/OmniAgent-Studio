package com.devmind.studio.controller;

import org.springframework.security.core.Authentication;

public abstract class BaseController {
    protected Long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            throw new IllegalStateException("缺少设备身份，请刷新页面后重试。");
        }
        return (Long) authentication.getPrincipal();
    }
}
