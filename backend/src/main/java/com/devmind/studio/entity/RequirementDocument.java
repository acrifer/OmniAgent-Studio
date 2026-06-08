package com.devmind.studio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "requirement_documents", indexes = @Index(name = "idx_document_project", columnList = "projectId"))
public class RequirementDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long projectId;

    private String fileName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private String parseStatus = "STORED";
    @Lob
    private String errorMessage;

    @Lob
    private String parsedText;

    private LocalDateTime createdAt = LocalDateTime.now();
}
