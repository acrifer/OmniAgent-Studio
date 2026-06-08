package com.devmind.studio.repository;

import com.devmind.studio.entity.AgentResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentResultRepository extends JpaRepository<AgentResult, Long> {
    List<AgentResult> findByTaskIdOrderByCreatedAtAsc(Long taskId);
    List<AgentResult> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    Optional<AgentResult> findTopByTaskIdAndAgentTypeOrderByCreatedAtDesc(Long taskId, String agentType);
}
