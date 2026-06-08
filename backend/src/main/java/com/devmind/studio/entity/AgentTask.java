package com.devmind.studio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "agent_tasks", indexes = {
        @Index(name = "idx_agent_task_project", columnList = "projectId"),
        @Index(name = "idx_agent_task_status", columnList = "status")
})
public class AgentTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 30)
    private String taskType = "FULL_ANALYSIS";

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    private Integer progress = 0;
    private String targetAgent;
    private String fastapiTaskId;
    private String langfuseTraceId;

    @Lob
    private String errorMessage;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
