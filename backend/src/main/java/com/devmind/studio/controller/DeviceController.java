package com.devmind.studio.controller;

import com.devmind.studio.dto.ApiResponse;
import com.devmind.studio.dto.DeviceDtos.DeviceQuotaResponse;
import com.devmind.studio.service.DeviceQuotaService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeviceController extends BaseController {
    private final DeviceQuotaService quotas;

    public DeviceController(DeviceQuotaService quotas) {
        this.quotas = quotas;
    }

    @GetMapping("/api/device/quota")
    public ApiResponse<DeviceQuotaResponse> quota(Authentication authentication) {
        return ApiResponse.ok(quotas.quota(currentUserId(authentication)));
    }
}
