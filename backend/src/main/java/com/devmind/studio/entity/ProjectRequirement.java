package com.devmind.studio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "project_requirements", indexes = @Index(name = "idx_requirement_project_version", columnList = "projectId,version"))
public class ProjectRequirement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long projectId;

    @Lob
    @Column(nullable = false)
    private String requirementText;

    @Column(nullable = false, length = 20)
    private String sourceType = "TEXT";

    @Column(nullable = false)
    private Integer version = 1;

    private LocalDateTime createdAt = LocalDateTime.now();
}
