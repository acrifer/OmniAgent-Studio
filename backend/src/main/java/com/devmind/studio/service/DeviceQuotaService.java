package com.devmind.studio.service;

import com.devmind.studio.dto.DeviceDtos.AdminDeviceView;
import com.devmind.studio.dto.DeviceDtos.DeviceQuotaResponse;
import com.devmind.studio.dto.DeviceDtos.UpdateDeviceQuotaRequest;
import com.devmind.studio.entity.DeviceProfile;
import com.devmind.studio.repository.DeviceProfileRepository;
import com.devmind.studio.repository.TokenUsageRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeviceQuotaService {
    private final DeviceProfileRepository devices;
    private final TokenUsageRecordRepository tokenRecords;
    private final DeviceProfileService deviceProfileService;

    public DeviceQuotaService(DeviceProfileRepository devices,
                              TokenUsageRecordRepository tokenRecords,
                              DeviceProfileService deviceProfileService) {
        this.devices = devices;
        this.tokenRecords = tokenRecords;
        this.deviceProfileService = deviceProfileService;
    }

    public DeviceQuotaResponse quota(Long ownerId) {
        DeviceProfile device = devices.findById(ownerId).orElseThrow(() -> new IllegalArgumentException("设备不存在"));
        return toQuotaResponse(device, usedToday(ownerId));
    }

    public void assertCanStartRun(Long ownerId) {
        DeviceProfile device = devices.findById(ownerId).orElseThrow(() -> new IllegalArgumentException("设备不存在"));
        if (Boolean.TRUE.equals(device.getUnlimitedQuota())) {
            return;
        }
        int limit = effectiveLimit(device);
        int used = usedToday(ownerId);
        if (used >= limit) {
            throw new IllegalStateException("今日 Token 额度已用完，请联系管理员加入白名单或调高额度。");
        }
    }

    public List<AdminDeviceView> adminDevices(String keyword) {
        List<DeviceProfile> profiles = keyword == null || keyword.isBlank()
                ? devices.findTop200ByOrderByLastSeenAtDesc()
                : devices.findTop200ByDeviceIdContainingIgnoreCaseOrNoteContainingIgnoreCaseOrderByLastSeenAtDesc(keyword, keyword);
        return profiles.stream().map(device -> toAdminView(device, usedToday(device.getId()))).toList();
    }

    @Transactional
    public AdminDeviceView updateDevice(Long id, UpdateDeviceQuotaRequest request) {
        DeviceProfile device = devices.findById(id).orElseThrow(() -> new IllegalArgumentException("设备不存在"));
        if (request.dailyLimit() != null) {
            if (request.dailyLimit() < 0) {
                throw new IllegalArgumentException("每日额度不能小于 0");
            }
            device.setDailyTokenLimit(request.dailyLimit());
        }
        if (request.whitelistEnabled() != null) {
            device.setWhitelistEnabled(request.whitelistEnabled());
        }
        if (request.unlimitedQuota() != null) {
            device.setUnlimitedQuota(request.unlimitedQuota());
        }
        if (request.note() != null) {
            device.setNote(request.note().isBlank() ? null : request.note().trim());
        }
        DeviceProfile saved = devices.save(device);
        return toAdminView(saved, usedToday(saved.getId()));
    }

    @Transactional
    public DeviceQuotaResponse resetToday(Long id) {
        DeviceProfile device = devices.findById(id).orElseThrow(() -> new IllegalArgumentException("设备不存在"));
        tokenRecords.deleteByUserIdAndCreatedAtBetween(id, startOfToday(), startOfTomorrow());
        return toQuotaResponse(device, 0);
    }

    private int usedToday(Long ownerId) {
        Long used = tokenRecords.sumTotalTokensByUserIdAndCreatedAtBetween(ownerId, startOfToday(), startOfTomorrow());
        return used == null ? 0 : Math.toIntExact(used);
    }

    private DeviceQuotaResponse toQuotaResponse(DeviceProfile device, int used) {
        boolean unlimited = Boolean.TRUE.equals(device.getUnlimitedQuota());
        int limit = effectiveLimit(device);
        Integer remaining = unlimited ? null : Math.max(0, limit - used);
        return new DeviceQuotaResponse(
                device.getId(),
                device.getDeviceId(),
                shortId(device.getDeviceId()),
                used,
                unlimited ? null : limit,
                remaining,
                Boolean.TRUE.equals(device.getWhitelistEnabled()),
                unlimited,
                startOfTomorrow()
        );
    }

    private AdminDeviceView toAdminView(DeviceProfile device, int used) {
        DeviceQuotaResponse quota = toQuotaResponse(device, used);
        return new AdminDeviceView(
                device.getId(),
                device.getDeviceId(),
                quota.shortDeviceId(),
                quota.usedToday(),
                quota.dailyLimit(),
                quota.remainingToday(),
                quota.whitelistEnabled(),
                quota.unlimitedQuota(),
                device.getNote(),
                device.getCreatedAt(),
                device.getLastSeenAt()
        );
    }

    private int effectiveLimit(DeviceProfile device) {
        return device.getDailyTokenLimit() == null ? deviceProfileService.defaultDailyTokenLimit() : device.getDailyTokenLimit();
    }

    private String shortId(String deviceId) {
        return deviceId == null || deviceId.length() <= 8 ? deviceId : deviceId.substring(0, 8);
    }

    private LocalDateTime startOfToday() {
        return LocalDate.now().atStartOfDay();
    }

    private LocalDateTime startOfTomorrow() {
        return LocalDate.now().plusDays(1).atStartOfDay();
    }
}
