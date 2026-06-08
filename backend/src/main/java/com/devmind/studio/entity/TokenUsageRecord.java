package com.devmind.studio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "token_usage_records", indexes = {
        @Index(name = "idx_token_project_agent", columnList = "projectId,agentType"),
        @Index(name = "idx_token_task", columnList = "taskId")
})
public class TokenUsageRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long taskId;
    private Long projectId;
    private Long runId;
    private Long conversationId;
    private String agentType;
    private String modelName;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private BigDecimal cost = BigDecimal.ZERO;
    private Integer latencyMs;
    private LocalDateTime createdAt = LocalDateTime.now();
}
