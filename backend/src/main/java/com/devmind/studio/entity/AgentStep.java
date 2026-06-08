package com.devmind.studio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "agent_steps", indexes = @Index(name = "idx_agent_step_run", columnList = "runId,createdAt"))
public class AgentStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long runId;

    @Column(nullable = false)
    private Long conversationId;

    @Column(nullable = false, length = 40)
    private String agentType;

    @Column(nullable = false, length = 20)
    private String status;

    @Lob
    private String inputJson;

    @Lob
    private String outputJson;

    @Lob
    private String contentMarkdown;

    @Lob
    private String citationsJson;

    private String modelName;
    private String promptVersion;
    private String traceObservationId;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer latencyMs;

    @Lob
    private String errorMessage;

    private LocalDateTime createdAt = LocalDateTime.now();
}
