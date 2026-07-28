package com.homektv.ai;

import com.homektv.domain.Song;
import com.homektv.repo.SongRepository;
import com.homektv.web.ApiException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class AiClassificationApplier {
    private final SongRepository songRepository;

    public AiClassificationApplier(SongRepository songRepository) {
        this.songRepository = songRepository;
    }

    public Song apply(Long songId, AiSongClassification result) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new ApiException("SONG_NOT_FOUND", "歌曲不存在"));
        song.setAiLanguage(normalize(result.language()));
        song.setAiEra(normalize(result.era()));
        song.setAiGenres(clean(result.genres()));
        song.setAiThemes(clean(result.themes()));
        song.setAiAgeRange(normalize(result.ageRange()));
        song.setAiVocalForm(normalize(result.vocalForm()));
        song.setAiAnalyzedAt(OffsetDateTime.now());
        song.setTags(mergeTags(song.getTags(), result));
        if (result.language() != null && !result.language().isBlank() && !"未知".equals(result.language())) {
            song.setLanguage(result.language());
        }
        return songRepository.save(song);
    }

    private String[] mergeTags(String[] current, AiSongClassification result) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (current != null) tags.addAll(Arrays.asList(current));
        tags.addAll(result.genres());
        tags.addAll(result.themes());
        if (result.era() != null) tags.add(result.era());
        if (result.vocalForm() != null) tags.add(result.vocalForm());
        tags.removeIf(value -> value == null || value.isBlank() || "未知".equals(value));
        return tags.toArray(String[]::new);
    }

    private String[] clean(List<String> values) {
        return values.stream().map(String::trim).filter(value -> !value.isBlank()).distinct().limit(10).toArray(String[]::new);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "未知" : value.trim();
    }
}
