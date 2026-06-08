package com.devmind.studio.controller;

import org.springframework.security.core.Authentication;

public abstract class BaseController {
    protected Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
