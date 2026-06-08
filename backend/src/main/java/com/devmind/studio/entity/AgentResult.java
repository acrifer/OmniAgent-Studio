package com.devmind.studio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "agent_results", indexes = @Index(name = "idx_agent_result_task_agent", columnList = "taskId,agentType"))
public class AgentResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long taskId;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 30)
    private String agentType;

    @Column(nullable = false, length = 20)
    private String status;

    @Lob
    private String contentJson;

    @Lob
    private String contentMarkdown;

    private BigDecimal qualityScore;
    private String promptVersion;
    private String modelName;
    private String traceObservationId;

    @Lob
    private String errorMessage;

    private LocalDateTime createdAt = LocalDateTime.now();
}
