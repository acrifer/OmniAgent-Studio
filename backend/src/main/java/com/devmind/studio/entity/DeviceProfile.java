package com.devmind.studio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "device_profiles", indexes = {
        @Index(name = "idx_device_profile_device_id", columnList = "deviceId", unique = true),
        @Index(name = "idx_device_profile_last_seen", columnList = "lastSeenAt")
})
public class DeviceProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String deviceId;

    private Integer dailyTokenLimit;
    private Boolean whitelistEnabled = false;
    private Boolean unlimitedQuota = false;
    private String note;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime lastSeenAt = LocalDateTime.now();
}
