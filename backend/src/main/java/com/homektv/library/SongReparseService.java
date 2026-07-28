package com.homektv.library;

import com.homektv.domain.Song;
import com.homektv.domain.SongFile;
import com.homektv.repo.SongFileRepository;
import com.homektv.repo.SongRepository;
import com.homektv.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class SongReparseService {
    private final SongRepository songRepository;
    private final SongFileRepository fileRepository;

    public SongReparseService(SongRepository songRepository, SongFileRepository fileRepository) {
        this.songRepository = songRepository;
        this.fileRepository = fileRepository;
    }

    @Transactional(readOnly = true)
    public List<Preview> preview(List<Long> songIds, String rule) {
        validateRule(rule);
        List<Preview> result = new ArrayList<>();
        for (Long songId : songIds) {
            Song song = songRepository.findById(songId).orElse(null);
            if (song == null) continue;
            SongFile file = fileRepository.findBySongIdAndValidTrueOrderByPriorityDesc(songId).stream().findFirst()
                    .orElseGet(() -> fileRepository.findBySongIdOrderByPriorityDesc(songId).stream().findFirst().orElse(null));
            if (file == null) {
                result.add(new Preview(songId, song.getTitle(), song.getArtist(), null, null, null, false, "没有文件源"));
                continue;
            }
            String filename = Path.of(file.getFilePath()).getFileName().toString();
            ParsedMeta parsed = FilenameParser.parse(filename, rule);
            String artist = parsed.artist().isBlank() ? "未知歌手" : parsed.artist();
            result.add(new Preview(songId, song.getTitle(), song.getArtist(), filename,
                    parsed.title(), artist, parsed.recognized(), parsed.recognized() ? null : "文件名无法识别"));
        }
        return result;
    }

    @Transactional
    public ApplyResult apply(List<Long> songIds, String rule) {
        List<Preview> previews = preview(songIds, rule);
        int updated = 0;
        int skipped = 0;
        for (Preview preview : previews) {
            if (!preview.recognized() || preview.proposedTitle() == null || preview.proposedTitle().isBlank()) {
                skipped++;
                continue;
            }
            Song song = songRepository.findById(preview.songId()).orElse(null);
            if (song == null) { skipped++; continue; }
            song.setTitle(preview.proposedTitle());
            song.setArtist(preview.proposedArtist());
            song.setTitlePy(PinyinUtil.fullPinyin(preview.proposedTitle()));
            song.setTitleInit(PinyinUtil.initials(preview.proposedTitle()));
            song.setArtistPy(PinyinUtil.fullPinyin(preview.proposedArtist()));
            song.setArtistInit(PinyinUtil.initials(preview.proposedArtist()));
            song.setStatus("ok");
            songRepository.save(song);
            updated++;
        }
        return new ApplyResult(updated, skipped, previews);
    }

    private void validateRule(String rule) {
        if (!"artist_title".equals(rule) && !"title_artist".equals(rule)) {
            throw new ApiException("INVALID_REPARSE_RULE", "不支持的重解析规则");
        }
    }

    public record Preview(Long songId, String currentTitle, String currentArtist, String filename,
                          String proposedTitle, String proposedArtist, boolean recognized, String error) {}
    public record ApplyResult(int updated, int skipped, List<Preview> items) {}
}
