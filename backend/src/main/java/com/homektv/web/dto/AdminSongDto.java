package com.homektv.web.dto;

import com.homektv.domain.Song;
import com.homektv.domain.SongFile;

public record AdminSongDto(
        Long id,
        String title,
        String artist,
        String language,
        String[] tags,
        String mediaType,
        String lyricType,
        int durationMs,
        int playCount,
        String filePath,
        String importSource
) {
    public static AdminSongDto from(Song song, SongFile file) {
        String source = file == null ? "UNKNOWN" : file.isTranscodeRequired() ? "TRANSCODED" : "COPIED";
        return new AdminSongDto(song.getId(), song.getTitle(), song.getArtist(), song.getLanguage(), song.getTags(),
                song.getMediaType(), song.getLyricType(), song.getDurationMs(), song.getPlayCount(),
                file == null ? null : file.getFilePath(), source);
    }
}
