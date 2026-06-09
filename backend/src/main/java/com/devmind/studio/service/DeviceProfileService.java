package com.devmind.studio.service;

import com.devmind.studio.entity.DeviceProfile;
import com.devmind.studio.repository.DeviceProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
public class DeviceProfileService {
    private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{8,100}$");

    private final DeviceProfileRepository devices;
    private final int defaultDailyTokenLimit;

    public DeviceProfileService(DeviceProfileRepository devices,
                                @Value("${app.default-daily-token-limit:50000}") int defaultDailyTokenLimit) {
        this.devices = devices;
        this.defaultDailyTokenLimit = defaultDailyTokenLimit;
    }

    @Transactional
    public DeviceProfile resolve(String deviceId) {
        if (deviceId == null || !DEVICE_ID_PATTERN.matcher(deviceId).matches()) {
            throw new IllegalArgumentException("缺少有效设备标识，请刷新页面后重试。");
        }
        DeviceProfile device = devices.findByDeviceId(deviceId).orElseGet(() -> {
            DeviceProfile profile = new DeviceProfile();
            profile.setDeviceId(deviceId);
            profile.setDailyTokenLimit(defaultDailyTokenLimit);
            return devices.save(profile);
        });
        device.setLastSeenAt(LocalDateTime.now());
        return devices.save(device);
    }

    public int defaultDailyTokenLimit() {
        return defaultDailyTokenLimit;
    }
}
