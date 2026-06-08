package com.devmind.studio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "conversations", indexes = @Index(name = "idx_conversation_user", columnList = "userId,updatedAt"))
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 30)
    private String mode = "AUTO";

    private Long knowledgeBaseId;
    private String modelName = "deepseek-v4-flash";
    private String status = "ACTIVE";

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
