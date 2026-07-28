package com.homektv.repo;

import com.homektv.domain.PlaylistSong;
import com.homektv.domain.PlaylistSongId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlaylistSongRepository extends JpaRepository<PlaylistSong, PlaylistSongId> {
    List<PlaylistSong> findByPlaylistIdOrderBySortOrder(Long playlistId);
    void deleteByPlaylistIdAndManualFalse(Long playlistId);
    void deleteByPlaylistIdAndSongId(Long playlistId, Long songId);

    @Modifying
    @Query(value = "INSERT INTO playlist_songs (playlist_id, song_id, sort_order, manual) " +
            "VALUES (:playlistId, :songId, :sortOrder, true) " +
            "ON CONFLICT (playlist_id, song_id) DO NOTHING", nativeQuery = true)
    int insertManualIfAbsent(Long playlistId, Long songId, int sortOrder);

    @Query(value = "SELECT pg_advisory_xact_lock(-CAST(:playlistId AS bigint))", nativeQuery = true)
    void lockPlaylist(Long playlistId);
}
