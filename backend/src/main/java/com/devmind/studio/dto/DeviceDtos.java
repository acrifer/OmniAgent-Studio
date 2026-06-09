package com.devmind.studio.dto;

import java.time.LocalDateTime;

public class DeviceDtos {
    public record DeviceQuotaResponse(
            Long ownerId,
            String deviceId,
            String shortDeviceId,
            int usedToday,
            Integer dailyLimit,
            Integer remainingToday,
            boolean whitelistEnabled,
            boolean unlimitedQuota,
            LocalDateTime resetAt
    ) {}

    public record AdminDeviceView(
            Long id,
            String deviceId,
            String shortDeviceId,
            int usedToday,
            Integer dailyLimit,
            Integer remainingToday,
            boolean whitelistEnabled,
            boolean unlimitedQuota,
            String note,
            LocalDateTime createdAt,
            LocalDateTime lastSeenAt
    ) {}

    public record UpdateDeviceQuotaRequest(
            Integer dailyLimit,
            Boolean whitelistEnabled,
            Boolean unlimitedQuota,
            String note
    ) {}
}
