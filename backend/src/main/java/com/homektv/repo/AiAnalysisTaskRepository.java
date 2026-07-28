package com.homektv.repo;

import com.homektv.domain.AiAnalysisTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiAnalysisTaskRepository extends JpaRepository<AiAnalysisTask, Long> {
    List<AiAnalysisTask> findTop100ByOrderByCreatedAtDesc();
    boolean existsBySongIdAndStatusIn(Long songId, List<String> statuses);
}
