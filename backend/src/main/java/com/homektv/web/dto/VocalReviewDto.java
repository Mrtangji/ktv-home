package com.homektv.web.dto;

/**
 * 伴奏轨低置信度复核项（后台）：一条待人工确认原伴唱的文件源 + 所属歌曲信息。
 *
 * @param fileId          song_files.id（swap/确认时用）
 * @param songId          所属歌曲 id
 * @param title           歌名
 * @param artist          歌手
 * @param format          容器格式
 * @param audioTracks     音轨数
 * @param vocalTrackIndex 当前判定的伴奏轨 index（0-based，回落时通常为 1）
 * @param vocalConfidence 置信度（LOW）
 */
public record VocalReviewDto(
        Long fileId,
        Long songId,
        String title,
        String artist,
        String format,
        int audioTracks,
        Integer vocalTrackIndex,
        String vocalConfidence
) {}
