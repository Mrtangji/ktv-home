package com.homektv.repo;

import com.homektv.domain.MediaImportRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MediaImportRecordRepository extends JpaRepository<MediaImportRecord, Long> {
    Optional<MediaImportRecord> findBySourcePath(String sourcePath);
    boolean existsBySourceMd5(String sourceMd5);
    boolean existsBySourceMd5AndSourcePathNot(String sourceMd5, String sourcePath);
    boolean existsByOutputMd5(String outputMd5);
    boolean existsByOutputMd5AndSourcePathNot(String outputMd5, String sourcePath);
    Page<MediaImportRecord> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);
    Page<MediaImportRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<MediaImportRecord> findAllByOrderByCreatedAtDesc();
    List<MediaImportRecord> findByIdIn(Collection<Long> ids);
    List<MediaImportRecord> findBySongId(Long songId);
    List<MediaImportRecord> findBySongFileId(Long songFileId);
}
