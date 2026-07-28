package com.homektv.repo;

import com.homektv.domain.PlaylistSong;
import com.homektv.domain.PlaylistSongId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaylistSongRepository extends JpaRepository<PlaylistSong, PlaylistSongId> {
    List<PlaylistSong> findByPlaylistIdOrderBySortOrder(Long playlistId);
    void deleteByPlaylistIdAndManualFalse(Long playlistId);
    void deleteByPlaylistIdAndSongId(Long playlistId, Long songId);
}
