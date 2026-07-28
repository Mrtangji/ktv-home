package com.homektv.repo;

import com.homektv.domain.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    Optional<Playlist> findByName(String name);
    List<Playlist> findAllByOrderByUpdatedAtDesc();
}
