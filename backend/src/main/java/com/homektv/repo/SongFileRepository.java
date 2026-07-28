package com.homektv.repo;

import com.homektv.domain.SongFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SongFileRepository extends JpaRepository<SongFile, Long> {

    List<SongFile> findBySongIdOrderByPriorityDesc(Long songId);

    Optional<SongFile> findByFilePath(String filePath);
    List<SongFile> findBySongIdAndValidTrueOrderByPriorityDesc(Long songId);
    boolean existsBySourceMd5(String sourceMd5);
    boolean existsByOutputMd5(String outputMd5);
    List<SongFile> findByFileRoleOrderByImportedAtDesc(String fileRole);
    List<SongFile> findBySourcePath(String sourcePath);

    /** 伴奏轨判定为低置信度的文件源，供后台人工复核（详设§11：入库记伴奏轨，判不准的挑出来核对）。 */
    Page<SongFile> findByVocalConfidence(String vocalConfidence, Pageable pageable);
}
