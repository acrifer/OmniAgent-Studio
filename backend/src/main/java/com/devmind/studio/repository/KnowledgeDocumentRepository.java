package com.devmind.studio.repository;

import com.devmind.studio.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {
    List<KnowledgeDocument> findByKnowledgeBaseIdOrderByCreatedAtDesc(Long knowledgeBaseId);
}
