package com.devmind.studio.repository;

import com.devmind.studio.entity.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {
    List<PromptTemplate> findAllByOrderByAgentTypeAscCreatedAtDesc();
}
