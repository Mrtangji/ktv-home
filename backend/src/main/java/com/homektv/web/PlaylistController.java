package com.homektv.web;

import com.homektv.library.PlaylistPublicService;
import com.homektv.config.AppProperties;
import com.homektv.repo.PlaylistRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {
    private final PlaylistPublicService service;
    private final PlaylistRepository playlistRepository;
    private final Path dataRoot;

    public PlaylistController(PlaylistPublicService service, PlaylistRepository playlistRepository, AppProperties properties) {
        this.service = service;
        this.playlistRepository = playlistRepository;
        this.dataRoot = Path.of(properties.getDataPath());
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping("/{id}/order")
    public Map<String, Object> orderAll(@PathVariable Long id, @RequestBody OrderPlaylistRequest request) {
        return service.orderAll(id, request.clientToken());
    }

    @GetMapping("/{id}/cover")
    public ResponseEntity<Resource> cover(@PathVariable Long id) {
        return playlistRepository.findById(id)
                .filter(playlist -> playlist.getCoverPath() != null)
                .map(playlist -> serveCover(playlist.getCoverPath()))
                .orElse(ResponseEntity.notFound().build());
    }

    private ResponseEntity<Resource> serveCover(String relativePath) {
        Path file = dataRoot.resolve(relativePath).normalize();
        if (!file.startsWith(dataRoot.normalize()) || !Files.isReadable(file)) return ResponseEntity.notFound().build();
        String name = file.getFileName().toString().toLowerCase();
        MediaType type = name.endsWith(".png") ? MediaType.IMAGE_PNG
                : name.endsWith(".webp") ? MediaType.parseMediaType("image/webp") : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).body(new FileSystemResource(file));
    }

    public record OrderPlaylistRequest(String clientToken) {}
}
