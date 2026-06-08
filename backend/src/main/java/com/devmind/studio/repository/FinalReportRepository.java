package com.devmind.studio.repository;

import com.devmind.studio.entity.FinalReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FinalReportRepository extends JpaRepository<FinalReport, Long> {
    Optional<FinalReport> findTopByProjectIdOrderByVersionDesc(Long projectId);
}
