package com.homektv.repo;

import com.homektv.domain.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    Optional<Playlist> findByName(String name);
    List<Playlist> findAllByOrderByUpdatedAtDesc();

    @Query(value = "SELECT pg_advisory_xact_lock(hashtextextended(:name, 0))", nativeQuery = true)
    void lockGeneratedName(String name);
}
