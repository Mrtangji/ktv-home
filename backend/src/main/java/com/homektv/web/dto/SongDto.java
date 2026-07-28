package com.homektv.web.dto;

import com.homektv.domain.Song;

/**
 * 歌曲列表/搜索结果项（详设§11.1）。
 */
public record SongDto(
        Long id,
        String title,
        String artist,
        String mediaType,
        boolean hasVocalTrack,
        int durationMs,
        String lyricType,
        String coverUrl,
        int playCount
) {
    public static SongDto from(Song s) {
        return new SongDto(
                s.getId(),
                s.getTitle(),
                s.getArtist(),
                s.getMediaType(),
                s.isHasVocalTrack(),
                s.getDurationMs(),
                s.getLyricType(),
                s.getCoverPath() != null ? "/api/cover/" + s.getId() : null,
                s.getPlayCount()
        );
    }
}
