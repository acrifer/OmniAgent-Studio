package com.devmind.studio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "model_providers", indexes = @Index(name = "idx_model_provider_user", columnList = "userId,provider"))
public class ModelProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(nullable = false, length = 100)
    private String modelName;

    private String baseUrl;
    private Boolean enabled = true;

    @Lob
    private String configJson;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
