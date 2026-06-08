package com.devmind.studio.repository;

import com.devmind.studio.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {
    List<UploadedFile> findByConversationIdOrderByCreatedAtDesc(Long conversationId);
    List<UploadedFile> findByKnowledgeBaseIdOrderByCreatedAtDesc(Long knowledgeBaseId);
}
