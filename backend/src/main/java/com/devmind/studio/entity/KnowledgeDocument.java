package com.devmind.studio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "knowledge_documents", indexes = @Index(name = "idx_kb_document", columnList = "knowledgeBaseId"))
public class KnowledgeDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long knowledgeBaseId;

    @Column(nullable = false)
    private Long userId;

    private Long uploadedFileId;
    private String fileName;
    private String ingestStatus = "PENDING";

    @Lob
    private String parsedTextPreview;

    @Lob
    private String errorMessage;

    private LocalDateTime createdAt = LocalDateTime.now();
}
