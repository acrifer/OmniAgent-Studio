package com.devmind.studio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "uploaded_files", indexes = @Index(name = "idx_uploaded_file_conversation", columnList = "conversationId"))
public class UploadedFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private Long conversationId;
    private Long knowledgeBaseId;

    private String fileName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private String parseStatus = "STORED";

    @Lob
    private String parsedText;

    @Lob
    private String errorMessage;

    private LocalDateTime createdAt = LocalDateTime.now();
}
