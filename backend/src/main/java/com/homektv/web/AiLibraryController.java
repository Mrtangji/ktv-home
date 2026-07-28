package com.homektv.web;

import com.homektv.ai.AiLibraryService;
import com.homektv.ai.AiSongClassification;
import com.homektv.domain.AiAnalysisTask;
import com.homektv.domain.Playlist;
import com.homektv.domain.Song;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ai")
public class AiLibraryController {
    private final AiLibraryService service;

    public AiLibraryController(AiLibraryService service) {
        this.service = service;
    }

    @GetMapping("/tasks")
    public List<AiAnalysisTask> tasks() {
        return service.listTasks();
    }

    @PostMapping("/tasks")
    public AiAnalysisTask create(@RequestBody CreateTaskRequest request) {
        return service.createTask(request.songId());
    }

    @PostMapping("/tasks/unclassified")
    public Map<String, Object> createUnclassified(@RequestBody(required = false) BatchRequest request) {
        int limit = request == null || request.limit() == null ? 50 : request.limit();
        List<AiAnalysisTask> tasks = service.createUnclassifiedTasks(limit);
        return Map.of("created", tasks.size(), "tasks", tasks);
    }

    @PostMapping("/tasks/{id}/retry")
    public AiAnalysisTask retry(@PathVariable Long id) {
        return service.retry(id);
    }

    @PostMapping("/tasks/{id}/apply")
    public Song apply(@PathVariable Long id, @RequestBody(required = false) AiSongClassification result) {
        return service.apply(id, result);
    }

    @PostMapping("/playlists/generate")
    public Playlist generatePlaylist(@RequestBody GeneratePlaylistRequest request) {
        return service.generatePlaylist(request.name(), request.tag(), request.limit() == null ? 100 : request.limit());
    }

    @GetMapping("/playlists")
    public List<Map<String, Object>> playlists() {
        return service.listPlaylists();
    }

    @GetMapping("/playlists/{id}")
    public Map<String, Object> playlist(@PathVariable Long id) {
        return service.playlistDetail(id);
    }

    @PostMapping("/playlists")
    public Playlist createPlaylist(@RequestBody SavePlaylistRequest request) {
        return service.savePlaylist(null, request.name(), request.description(), request.theme(), request.publicVisible());
    }

    @PutMapping("/playlists/{id}")
    public Playlist updatePlaylist(@PathVariable Long id, @RequestBody SavePlaylistRequest request) {
        return service.savePlaylist(id, request.name(), request.description(), request.theme(), request.publicVisible());
    }

    @DeleteMapping("/playlists/{id}")
    public void deletePlaylist(@PathVariable Long id) {
        service.deletePlaylist(id);
    }

    @PostMapping("/playlists/{id}/songs")
    public Map<String, Object> addSong(@PathVariable Long id, @RequestBody PlaylistSongRequest request) {
        return service.addPlaylistSong(id, request.songId());
    }

    @DeleteMapping("/playlists/{id}/songs/{songId}")
    public void removeSong(@PathVariable Long id, @PathVariable Long songId) {
        service.removePlaylistSong(id, songId);
    }

    @PostMapping(value = "/playlists/{id}/cover", consumes = "multipart/form-data")
    public Playlist uploadCover(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return service.uploadPlaylistCover(id, file);
    }

    @PutMapping("/playlists/{id}/songs/order")
    public Map<String, Object> reorderSongs(@PathVariable Long id, @RequestBody ReorderSongsRequest request) {
        return service.reorderPlaylistSongs(id, request.songIds());
    }

    public record CreateTaskRequest(Long songId) {}
    public record BatchRequest(Integer limit) {}
    public record GeneratePlaylistRequest(String name, String tag, Integer limit) {}
    public record SavePlaylistRequest(String name, String description, String theme, boolean publicVisible) {}
    public record PlaylistSongRequest(Long songId) {}
    public record ReorderSongsRequest(List<Long> songIds) {}
}
