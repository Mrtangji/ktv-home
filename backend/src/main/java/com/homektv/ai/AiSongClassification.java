package com.homektv.ai;

import java.util.List;

public record AiSongClassification(
        String language,
        String era,
        List<String> genres,
        List<String> themes,
        String ageRange,
        String vocalForm,
        List<String> recommendedPlaylists,
        String reason,
        Double confidence
) {
    public AiSongClassification {
        genres = genres == null ? List.of() : List.copyOf(genres);
        themes = themes == null ? List.of() : List.copyOf(themes);
        recommendedPlaylists = recommendedPlaylists == null ? List.of() : List.copyOf(recommendedPlaylists);
        confidence = confidence == null ? 0.0 : Math.max(0.0, Math.min(1.0, confidence));
    }
}
