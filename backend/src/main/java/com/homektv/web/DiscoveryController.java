package com.homektv.web;

import com.homektv.domain.Song;
import com.homektv.repo.PlayHistoryRepository;
import com.homektv.repo.SongRepository;
import com.homektv.web.dto.SongDto;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 发现类 API（P3.4）：点唱排行 / 最新入库。供 H5 首页热榜与分类使用。
 */
@RestController
@RequestMapping("/api")
public class DiscoveryController {

    private final PlayHistoryRepository historyRepo;
    private final SongRepository songRepo;

    public DiscoveryController(PlayHistoryRepository historyRepo, SongRepository songRepo) {
        this.historyRepo = historyRepo;
        this.songRepo = songRepo;
    }

    /** 点唱排行（P3.4）：默认近 30 天 Top 20 */
    @GetMapping("/ranking")
    public List<SongDto> ranking(@RequestParam(defaultValue = "30") int days) {
        OffsetDateTime since = OffsetDateTime.now().minusDays(days);
        List<Object[]> rows = historyRepo.ranking(since, 20);
        List<SongDto> out = new ArrayList<>();
        for (Object[] row : rows) {
            Long songId = ((Number) row[0]).longValue();
            songRepo.findById(songId)
                    .filter(s -> "ok".equals(s.getStatus()))
                    .ifPresent(s -> out.add(SongDto.from(s)));
        }
        return out;
    }

    /** 最新入库（P3.4） */
    @GetMapping("/songs/new")
    public List<SongDto> newSongs() {
        return songRepo.findTop50ByOrderByCreatedAtDesc().stream()
                .filter(s -> "ok".equals(s.getStatus()))
                .map(SongDto::from)
                .toList();
    }
}
