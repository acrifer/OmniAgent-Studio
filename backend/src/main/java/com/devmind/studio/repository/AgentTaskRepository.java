package com.devmind.studio.repository;

import com.devmind.studio.entity.AgentTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentTaskRepository extends JpaRepository<AgentTask, Long> {
    Optional<AgentTask> findByFastapiTaskId(String fastapiTaskId);
    List<AgentTask> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    Optional<AgentTask> findTopByProjectIdOrderByCreatedAtDesc(Long projectId);
}
