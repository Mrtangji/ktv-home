package com.homektv.repo;

import com.homektv.domain.Song;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SongRepository extends JpaRepository<Song, Long> {

    Optional<Song> findByFingerprint(String fingerprint);

    long countByMediaType(String mediaType);

    long countByStatus(String status);

    java.util.List<Song> findTop50ByOrderByCreatedAtDesc();

    org.springframework.data.domain.Page<Song> findByMediaType(String mediaType, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Song> findByStatus(String status, org.springframework.data.domain.Pageable pageable);
}
