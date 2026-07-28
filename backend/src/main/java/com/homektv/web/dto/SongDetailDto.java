package com.homektv.web.dto;

import com.homektv.domain.Song;
import com.homektv.domain.SongFile;

import java.util.List;

/**
 * 歌曲详情（详设§11.1）：含歌词 URL、可用文件源/音轨信息。
 */
public record SongDetailDto(
        Long id,
        String title,
        String artist,
        String language,
        String[] tags,
        String mediaType,
        boolean hasVocalTrack,
        int durationMs,
        String lyricType,
        String coverUrl,
        String lyricUrl,
        int playCount,
        List<FileSourceDto> files
) {
    public record FileSourceDto(Long id, String format, int audioTracks, Integer vocalTrackIndex,
                                String vocalConfidence, String resolution, int priority) {
        static FileSourceDto from(SongFile f) {
            return new FileSourceDto(f.getId(), f.getFormat(), f.getAudioTracks(),
                    f.getVocalTrackIndex(), f.getVocalConfidence(), f.getResolution(), f.getPriority());
        }
    }

    public static SongDetailDto from(Song s, List<SongFile> files) {
        return new SongDetailDto(
                s.getId(), s.getTitle(), s.getArtist(), s.getLanguage(), s.getTags(),
                s.getMediaType(), s.isHasVocalTrack(), s.getDurationMs(), s.getLyricType(),
                s.getCoverPath() != null ? "/api/cover/" + s.getId() : null,
                !"none".equals(s.getLyricType()) ? "/api/lyric/" + s.getId() : null,
                s.getPlayCount(),
                files.stream().map(FileSourceDto::from).toList()
        );
    }
}
