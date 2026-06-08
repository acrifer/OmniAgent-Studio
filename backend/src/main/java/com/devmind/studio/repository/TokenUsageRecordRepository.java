package com.devmind.studio.repository;

import com.devmind.studio.entity.TokenUsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TokenUsageRecordRepository extends JpaRepository<TokenUsageRecord, Long> {
    List<TokenUsageRecord> findByProjectId(Long projectId);
    List<TokenUsageRecord> findByTaskIdOrderByCreatedAtAsc(Long taskId);
    List<TokenUsageRecord> findByConversationId(Long conversationId);
    List<TokenUsageRecord> findByRunIdOrderByCreatedAtAsc(Long runId);
}
