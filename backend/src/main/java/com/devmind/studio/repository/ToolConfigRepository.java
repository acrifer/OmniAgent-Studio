package com.devmind.studio.repository;

import com.devmind.studio.entity.ToolConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToolConfigRepository extends JpaRepository<ToolConfig, Long> {
    List<ToolConfig> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
