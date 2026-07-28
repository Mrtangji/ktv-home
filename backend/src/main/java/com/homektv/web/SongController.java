package com.homektv.web;

import com.homektv.config.AppProperties;
import com.homektv.domain.Song;
import com.homektv.library.SongSearchService;
import com.homektv.repo.SongFileRepository;
import com.homektv.repo.SongRepository;
import com.homektv.web.dto.SongDetailDto;
import com.homektv.web.dto.SongDto;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 歌曲搜索/详情/资源 API（P1.6/P1.7，详设§11.1）。
 */
@RestController
@RequestMapping("/api")
public class SongController {

    private final SongSearchService searchService;
    private final SongRepository songRepo;
    private final SongFileRepository fileRepo;
    private final Path dataRoot;

    public SongController(SongSearchService searchService, SongRepository songRepo,
                          SongFileRepository fileRepo, AppProperties props) {
        this.searchService = searchService;
        this.songRepo = songRepo;
        this.fileRepo = fileRepo;
        this.dataRoot = Path.of(props.getDataPath());
    }

    /** 综合搜索（P1.6）：keyword 支持中文/全拼/首字母 */
    @GetMapping("/songs")
    public List<SongDto> search(@RequestParam(defaultValue = "") String keyword,
                                @RequestParam(defaultValue = "") String type,
                                @RequestParam(defaultValue = "0") int page) {
        List<Song> songs = searchService.search(keyword, page);
        return songs.stream()
                .filter(s -> type.isBlank() || s.getMediaType().equalsIgnoreCase(type))
                .map(SongDto::from)
                .toList();
    }

    /** 歌曲详情（P1.7） */
    @GetMapping("/songs/{id}")
    public ResponseEntity<SongDetailDto> detail(@PathVariable Long id) {
        return songRepo.findById(id)
                .map(s -> ResponseEntity.ok(
                        SongDetailDto.from(s, fileRepo.findBySongIdAndValidTrueOrderByPriorityDesc(id))))
                .orElse(ResponseEntity.notFound().build());
    }

    /** 封面资源（P1.7） */
    @GetMapping("/cover/{id}")
    public ResponseEntity<Resource> cover(@PathVariable Long id) {
        return songRepo.findById(id)
                .filter(s -> s.getCoverPath() != null)
                .map(s -> serveFile(s.getCoverPath(), guessImageType(s.getCoverPath())))
                .orElse(ResponseEntity.notFound().build());
    }

    /** 歌词资源（P1.7）：返回原始歌词文本（LRC/KSC）；JSON 时间轴解析留 P1.26 */
    @GetMapping(value = "/lyric/{id}", produces = "text/plain;charset=UTF-8")
    public ResponseEntity<Resource> lyric(@PathVariable Long id) {
        return songRepo.findById(id)
                .filter(s -> s.getLyricPath() != null)
                .map(s -> serveFile(s.getLyricPath(), MediaType.TEXT_PLAIN))
                .orElse(ResponseEntity.notFound().build());
    }

    private ResponseEntity<Resource> serveFile(String relPath, MediaType type) {
        Path file = dataRoot.resolve(relPath).normalize();
        // 防目录穿越：必须仍在 dataRoot 下
        if (!file.startsWith(dataRoot.normalize()) || !Files.isReadable(file)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().contentType(type).body(new FileSystemResource(file));
    }

    private static MediaType guessImageType(String path) {
        String p = path.toLowerCase();
        if (p.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (p.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }
}
