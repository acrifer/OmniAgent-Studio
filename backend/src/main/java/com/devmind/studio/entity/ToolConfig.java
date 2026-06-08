package com.devmind.studio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tool_configs", indexes = @Index(name = "idx_tool_config_user", columnList = "userId,toolType"))
public class ToolConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 30)
    private String toolType;

    @Column(nullable = false, length = 120)
    private String name;

    private String endpoint;
    private Boolean enabled = true;

    @Lob
    private String configJson;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
