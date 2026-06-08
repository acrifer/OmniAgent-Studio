package com.devmind.studio.repository;

import com.devmind.studio.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByUserIdOrderByUpdatedAtDesc(Long userId);
    Optional<Project> findByIdAndUserId(Long id, Long userId);
}
