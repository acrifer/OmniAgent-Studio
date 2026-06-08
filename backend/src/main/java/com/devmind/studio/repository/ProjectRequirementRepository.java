package com.devmind.studio.repository;

import com.devmind.studio.entity.ProjectRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectRequirementRepository extends JpaRepository<ProjectRequirement, Long> {
    Optional<ProjectRequirement> findTopByProjectIdOrderByVersionDesc(Long projectId);
}
