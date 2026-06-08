package com.devmind.studio.repository;

import com.devmind.studio.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {
    List<KnowledgeBase> findByUserIdOrderByUpdatedAtDesc(Long userId);
    Optional<KnowledgeBase> findByIdAndUserId(Long id, Long userId);
}
