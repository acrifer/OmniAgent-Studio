package com.devmind.studio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "user_feedback", indexes = @Index(name = "idx_feedback_project", columnList = "projectId"))
public class UserFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long projectId;
    private Long taskId;
    private String targetType;
    private Long targetId;
    private Integer rating;
    private String comment;
    private String langfuseScoreId;
    private LocalDateTime createdAt = LocalDateTime.now();
}
