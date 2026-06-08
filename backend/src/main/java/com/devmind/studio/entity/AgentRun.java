package com.devmind.studio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "agent_runs", indexes = {
        @Index(name = "idx_agent_run_conversation", columnList = "conversationId"),
        @Index(name = "idx_agent_run_status", columnList = "status")
})
public class AgentRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long conversationId;

    @Column(nullable = false)
    private Long userId;

    private Long messageId;
    private Long knowledgeBaseId;
    private String mode = "AUTO";
    private String status = "PENDING";
    private Integer progress = 0;
    private String modelName = "deepseek-v4-flash";
    private String aiTaskId;
    private String langfuseTraceId;

    @Lob
    private String question;

    @Lob
    private String answerMarkdown;

    @Lob
    private String citationsJson;

    @Lob
    private String usedToolsJson;

    private Double confidence;

    @Lob
    private String followUpQuestionsJson;

    @Lob
    private String errorMessage;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
