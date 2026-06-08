package com.devmind.studio.repository;

import com.devmind.studio.entity.ModelProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModelProviderRepository extends JpaRepository<ModelProvider, Long> {
    List<ModelProvider> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
