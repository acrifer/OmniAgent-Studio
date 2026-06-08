package com.devmind.studio.repository;

import com.devmind.studio.entity.AgentRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentRunRepository extends JpaRepository<AgentRun, Long> {
    Optional<AgentRun> findByAiTaskId(String aiTaskId);
    Optional<AgentRun> findTopByConversationIdOrderByCreatedAtDesc(Long conversationId);
    List<AgentRun> findByConversationIdOrderByCreatedAtDesc(Long conversationId);
    List<AgentRun> findByUserIdOrderByCreatedAtDesc(Long userId);
}
