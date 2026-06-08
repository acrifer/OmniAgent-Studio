package com.devmind.studio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "final_reports", indexes = @Index(name = "idx_report_project_version", columnList = "projectId,version"))
public class FinalReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long taskId;

    private String title;

    @Lob
    private String contentMarkdown;

    @Lob
    private String contentJson;

    private Integer version = 1;
    private LocalDateTime createdAt = LocalDateTime.now();
}
