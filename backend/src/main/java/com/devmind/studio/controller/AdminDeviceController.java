package com.devmind.studio.controller;

import com.devmind.studio.dto.ApiResponse;
import com.devmind.studio.dto.DeviceDtos.AdminDeviceView;
import com.devmind.studio.dto.DeviceDtos.DeviceQuotaResponse;
import com.devmind.studio.dto.DeviceDtos.UpdateDeviceQuotaRequest;
import com.devmind.studio.service.DeviceQuotaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminDeviceController {
    private final DeviceQuotaService quotas;
    private final String adminToken;

    public AdminDeviceController(DeviceQuotaService quotas,
                                 @Value("${app.admin-token:}") String adminToken) {
        this.quotas = quotas;
        this.adminToken = adminToken;
    }

    @GetMapping("/devices")
    public ApiResponse<List<AdminDeviceView>> devices(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                                      @RequestParam(required = false) String keyword) {
        requireAdmin(token);
        return ApiResponse.ok(quotas.adminDevices(keyword));
    }

    @PatchMapping("/devices/{id}/quota")
    public ApiResponse<AdminDeviceView> update(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                               @PathVariable Long id,
                                               @RequestBody UpdateDeviceQuotaRequest request) {
        requireAdmin(token);
        return ApiResponse.ok(quotas.updateDevice(id, request));
    }

    @PostMapping("/devices/{id}/reset-today")
    public ApiResponse<DeviceQuotaResponse> resetToday(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                                       @PathVariable Long id) {
        requireAdmin(token);
        return ApiResponse.ok(quotas.resetToday(id));
    }

    private void requireAdmin(String token) {
        if (adminToken == null || adminToken.isBlank()) {
            throw new IllegalStateException("管理员密钥未配置，请设置 ADMIN_TOKEN 或 app.admin-token。");
        }
        if (!adminToken.equals(token)) {
            throw new SecurityException("管理员密钥无效");
        }
    }
}
