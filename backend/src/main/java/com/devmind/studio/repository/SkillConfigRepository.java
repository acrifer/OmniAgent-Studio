package com.devmind.studio.repository;

import com.devmind.studio.entity.SkillConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillConfigRepository extends JpaRepository<SkillConfig, Long> {
    List<SkillConfig> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
