package com.homektv.domain;

import java.io.Serializable;
import java.util.Objects;

public class PlaylistSongId implements Serializable {
    private Long playlistId;
    private Long songId;

    public PlaylistSongId() {}
    public PlaylistSongId(Long playlistId, Long songId) {
        this.playlistId = playlistId;
        this.songId = songId;
    }
    public boolean equals(Object value) {
        if (this == value) return true;
        if (!(value instanceof PlaylistSongId other)) return false;
        return Objects.equals(playlistId, other.playlistId) && Objects.equals(songId, other.songId);
    }
    public int hashCode() { return Objects.hash(playlistId, songId); }
}
