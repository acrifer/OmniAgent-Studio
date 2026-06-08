package com.devmind.studio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "prompt_templates", indexes = @Index(name = "idx_prompt_agent_active", columnList = "agentType,isActive"))
public class PromptTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String agentType;

    @Column(nullable = false, length = 100)
    private String name;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false, length = 50)
    private String version;

    private Boolean isActive = true;
    private String modelName = "deepseek-v4-flash";
    private BigDecimal temperature = BigDecimal.valueOf(0.2);
    private Long createdBy;
    private LocalDateTime createdAt = LocalDateTime.now();
}
