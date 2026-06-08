package com.devmind.studio.repository;

import com.devmind.studio.entity.AgentStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentStepRepository extends JpaRepository<AgentStep, Long> {
    List<AgentStep> findByRunIdOrderByCreatedAtAsc(Long runId);
}
