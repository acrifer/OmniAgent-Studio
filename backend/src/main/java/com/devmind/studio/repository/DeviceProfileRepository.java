package com.devmind.studio.repository;

import com.devmind.studio.entity.DeviceProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceProfileRepository extends JpaRepository<DeviceProfile, Long> {
    Optional<DeviceProfile> findByDeviceId(String deviceId);
    List<DeviceProfile> findTop200ByOrderByLastSeenAtDesc();
    List<DeviceProfile> findTop200ByDeviceIdContainingIgnoreCaseOrNoteContainingIgnoreCaseOrderByLastSeenAtDesc(String deviceId, String note);
}
