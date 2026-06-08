package com.devmind.studio.repository;

import com.devmind.studio.entity.RequirementDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequirementDocumentRepository extends JpaRepository<RequirementDocument, Long> {
    List<RequirementDocument> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
