package com.devmind.studio.repository;

import com.devmind.studio.entity.TokenUsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface TokenUsageRecordRepository extends JpaRepository<TokenUsageRecord, Long> {
    List<TokenUsageRecord> findByProjectId(Long projectId);
    List<TokenUsageRecord> findByTaskIdOrderByCreatedAtAsc(Long taskId);
    List<TokenUsageRecord> findByConversationId(Long conversationId);
    List<TokenUsageRecord> findByRunIdOrderByCreatedAtAsc(Long runId);

    @Query("select coalesce(sum(r.totalTokens), 0) from TokenUsageRecord r where r.userId = :userId and r.createdAt >= :start and r.createdAt < :end")
    Long sumTotalTokensByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    @Modifying
    void deleteByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
}
