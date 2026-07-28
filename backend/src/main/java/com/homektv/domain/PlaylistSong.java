package com.homektv.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "playlist_songs")
@IdClass(PlaylistSongId.class)
public class PlaylistSong {
    @Id
    @Column(name = "playlist_id")
    private Long playlistId;
    @Id
    @Column(name = "song_id")
    private Long songId;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    @Column(nullable = false)
    private boolean manual;

    public Long getPlaylistId() { return playlistId; }
    public void setPlaylistId(Long playlistId) { this.playlistId = playlistId; }
    public Long getSongId() { return songId; }
    public void setSongId(Long songId) { this.songId = songId; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public boolean isManual() { return manual; }
    public void setManual(boolean manual) { this.manual = manual; }
}
