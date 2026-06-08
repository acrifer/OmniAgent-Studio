package com.devmind.studio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "messages", indexes = @Index(name = "idx_message_conversation", columnList = "conversationId,createdAt"))
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long conversationId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String role;

    @Lob
    private String content;

    @Lob
    private String metadataJson;

    private LocalDateTime createdAt = LocalDateTime.now();
}
