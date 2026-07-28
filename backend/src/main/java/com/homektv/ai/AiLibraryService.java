package com.homektv.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homektv.config.AppProperties;
import com.homektv.domain.AiAnalysisTask;
import com.homektv.domain.Playlist;
import com.homektv.domain.PlaylistSong;
import com.homektv.domain.Song;
import com.homektv.repo.AiAnalysisTaskRepository;
import com.homektv.repo.PlaylistRepository;
import com.homektv.repo.PlaylistSongRepository;
import com.homektv.repo.SongRepository;
import com.homektv.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.homektv.library.AssetWriter;

import java.io.IOException;

import java.util.*;

@Service
public class AiLibraryService {
    private static final List<String> ACTIVE_STATUSES = List.of("pending", "processing", "review");

    private final AiAnalysisTaskRepository taskRepository;
    private final SongRepository songRepository;
    private final PlaylistRepository playlistRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final AiAnalysisWorker worker;
    private final ObjectMapper objectMapper;
    private final AppProperties properties;
    private final AssetWriter assetWriter;
    private final AiClassificationApplier classificationApplier;

    public AiLibraryService(AiAnalysisTaskRepository taskRepository, SongRepository songRepository,
                            PlaylistRepository playlistRepository, PlaylistSongRepository playlistSongRepository,
                            AiAnalysisWorker worker, ObjectMapper objectMapper, AppProperties properties,
                            AssetWriter assetWriter, AiClassificationApplier classificationApplier) {
        this.taskRepository = taskRepository;
        this.songRepository = songRepository;
        this.playlistRepository = playlistRepository;
        this.playlistSongRepository = playlistSongRepository;
        this.worker = worker;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.assetWriter = assetWriter;
        this.classificationApplier = classificationApplier;
    }

    public AiAnalysisTask createTask(Long songId) {
        requireAiConfigured();
        songRepository.findById(songId).orElseThrow(() -> new ApiException("SONG_NOT_FOUND", "歌曲不存在"));
        if (taskRepository.existsBySongIdAndStatusIn(songId, ACTIVE_STATUSES)) {
            throw new ApiException("AI_TASK_EXISTS", "该歌曲已有待处理的 AI 任务");
        }
        AiAnalysisTask task = new AiAnalysisTask();
        task.setSongId(songId);
        task.setModel(properties.getAi().getModel());
        task = taskRepository.save(task);
        worker.analyze(task.getId());
        return task;
    }

    public List<AiAnalysisTask> createUnclassifiedTasks(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        List<AiAnalysisTask> result = new ArrayList<>();
        for (Song song : songRepository.findAll()) {
            if (result.size() >= safeLimit) break;
            if (song.getAiAnalyzedAt() == null && !taskRepository.existsBySongIdAndStatusIn(song.getId(), ACTIVE_STATUSES)) {
                result.add(createTask(song.getId()));
            }
        }
        return result;
    }

    public List<AiAnalysisTask> listTasks() {
        return taskRepository.findTop100ByOrderByCreatedAtDesc();
    }

    public List<Map<String, Object>> listPlaylists() {
        return playlistRepository.findAllByOrderByUpdatedAtDesc().stream().map(playlist -> {
            List<PlaylistSong> items = playlistSongRepository.findByPlaylistIdOrderBySortOrder(playlist.getId());
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("id", playlist.getId());
            value.put("name", playlist.getName());
            value.put("description", playlist.getDescription());
            value.put("theme", playlist.getTheme());
            value.put("coverUrl", playlist.getCoverPath() == null ? null : "/api/playlists/" + playlist.getId() + "/cover");
            value.put("publicVisible", playlist.isPublicVisible());
            value.put("aiGenerated", playlist.isAiGenerated());
            value.put("songCount", items.size());
            value.put("manualCount", items.stream().filter(PlaylistSong::isManual).count());
            value.put("updatedAt", playlist.getUpdatedAt());
            return value;
        }).toList();
    }

    public Map<String, Object> playlistDetail(Long playlistId) {
        Playlist playlist = requirePlaylist(playlistId);
        List<Map<String, Object>> songs = playlistSongRepository.findByPlaylistIdOrderBySortOrder(playlistId).stream()
                .map(item -> {
                    Song song = songRepository.findById(item.getSongId()).orElse(null);
                    Map<String, Object> value = new LinkedHashMap<>();
                    value.put("songId", item.getSongId());
                    value.put("title", song == null ? "歌曲已删除" : song.getTitle());
                    value.put("artist", song == null ? "" : song.getArtist());
                    value.put("manual", item.isManual());
                    value.put("sortOrder", item.getSortOrder());
                    return value;
                }).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", playlist.getId());
        result.put("name", playlist.getName());
        result.put("description", playlist.getDescription());
        result.put("theme", playlist.getTheme());
        result.put("coverPath", playlist.getCoverPath());
        result.put("coverUrl", playlist.getCoverPath() == null ? null : "/api/playlists/" + playlist.getId() + "/cover");
        result.put("publicVisible", playlist.isPublicVisible());
        result.put("aiGenerated", playlist.isAiGenerated());
        result.put("aiRule", playlist.getAiRule());
        result.put("songs", songs);
        return result;
    }

    @Transactional
    public Playlist savePlaylist(Long playlistId, String name, String description, String theme, boolean publicVisible) {
        if (name == null || name.isBlank()) throw new ApiException("INVALID_ARGUMENT", "歌单名称不能为空");
        Playlist playlist = playlistId == null ? new Playlist() : requirePlaylist(playlistId);
        playlist.setName(name.trim());
        playlist.setDescription(description == null ? "" : description.trim());
        playlist.setTheme(theme == null || theme.isBlank() ? null : theme.trim());
        playlist.setPublicVisible(publicVisible);
        return playlistRepository.save(playlist);
    }

    @Transactional
    public void deletePlaylist(Long playlistId) {
        playlistRepository.delete(requirePlaylist(playlistId));
    }

    @Transactional
    public Map<String, Object> addPlaylistSong(Long playlistId, Long songId) {
        requirePlaylist(playlistId);
        Song song = songRepository.findById(songId).orElseThrow(() -> new ApiException("SONG_NOT_FOUND", "歌曲不存在"));
        List<PlaylistSong> current = playlistSongRepository.findByPlaylistIdOrderBySortOrder(playlistId);
        if (current.stream().anyMatch(item -> item.getSongId().equals(songId))) return playlistDetail(playlistId);
        PlaylistSong item = new PlaylistSong();
        item.setPlaylistId(playlistId);
        item.setSongId(song.getId());
        item.setSortOrder(current.stream().mapToInt(PlaylistSong::getSortOrder).max().orElse(-1) + 1);
        item.setManual(true);
        playlistSongRepository.save(item);
        return playlistDetail(playlistId);
    }

    @Transactional
    public void removePlaylistSong(Long playlistId, Long songId) {
        requirePlaylist(playlistId);
        playlistSongRepository.deleteByPlaylistIdAndSongId(playlistId, songId);
    }

    @Transactional
    public Playlist uploadPlaylistCover(Long playlistId, MultipartFile file) {
        Playlist playlist = requirePlaylist(playlistId);
        if (file == null || file.isEmpty()) throw new ApiException("INVALID_IMAGE", "请选择封面图片");
        if (file.getSize() > 5 * 1024 * 1024) throw new ApiException("IMAGE_TOO_LARGE", "封面图片不能超过 5MB");
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        String extension = switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/jpeg", "image/jpg" -> "jpg";
            default -> throw new ApiException("INVALID_IMAGE", "仅支持 JPG、PNG 或 WebP 图片");
        };
        try {
            playlist.setCoverPath(assetWriter.writePlaylistCover(playlistId, file.getBytes(), extension));
            return playlistRepository.save(playlist);
        } catch (IOException e) {
            throw new ApiException("IMAGE_WRITE_FAILED", "封面保存失败");
        }
    }

    @Transactional
    public Map<String, Object> reorderPlaylistSongs(Long playlistId, List<Long> songIds) {
        requirePlaylist(playlistId);
        List<PlaylistSong> current = playlistSongRepository.findByPlaylistIdOrderBySortOrder(playlistId);
        if (songIds == null || songIds.size() != current.size() || new HashSet<>(songIds).size() != current.size()) {
            throw new ApiException("INVALID_PLAYLIST_ORDER", "排序歌曲列表不完整或包含重复项");
        }
        Map<Long, PlaylistSong> bySongId = new HashMap<>();
        current.forEach(item -> bySongId.put(item.getSongId(), item));
        for (int index = 0; index < songIds.size(); index++) {
            PlaylistSong item = bySongId.get(songIds.get(index));
            if (item == null) throw new ApiException("INVALID_PLAYLIST_ORDER", "排序中包含不属于该歌单的歌曲");
            item.setSortOrder(index);
        }
        playlistSongRepository.saveAll(current);
        return playlistDetail(playlistId);
    }

    public AiAnalysisTask retry(Long taskId) {
        requireAiConfigured();
        AiAnalysisTask task = requireTask(taskId);
        if ("pending".equals(task.getStatus()) || "processing".equals(task.getStatus())) {
            throw new ApiException("INVALID_AI_TASK", "任务正在处理中，不能重复分析");
        }
        task.setStatus("pending");
        task.setErrorMessage(null);
        taskRepository.save(task);
        worker.analyze(task.getId());
        return task;
    }

    @Transactional
    public Song apply(Long taskId, AiSongClassification override) {
        AiAnalysisTask task = requireTask(taskId);
        if (!"review".equals(task.getStatus())) throw new ApiException("INVALID_AI_TASK", "任务不处于待确认状态");
        AiSongClassification result = override == null ? parse(task.getResultJson()) : override;
        Song song = classificationApplier.apply(task.getSongId(), result);
        task.setStatus("applied");
        task.setResultJson(write(result));
        taskRepository.save(task);
        return song;
    }

    @Transactional
    public Playlist generatePlaylist(String name, String tag, int limit) {
        if (name == null || name.isBlank() || tag == null || tag.isBlank()) {
            throw new ApiException("INVALID_ARGUMENT", "歌单名称和 AI 标签不能为空");
        }
        Playlist playlist = playlistRepository.findByName(name.trim()).orElseGet(Playlist::new);
        playlist.setName(name.trim());
        playlist.setTheme(tag.trim());
        playlist.setDescription("由 AI 标签「" + tag.trim() + "」自动生成");
        playlist.setAiGenerated(true);
        playlist.setAiRule(write(Map.of("tag", tag.trim(), "limit", Math.max(1, Math.min(limit, 500)))));
        playlist = playlistRepository.save(playlist);

        playlistSongRepository.deleteByPlaylistIdAndManualFalse(playlist.getId());
        Set<Long> existing = new HashSet<>();
        int order = 0;
        for (PlaylistSong item : playlistSongRepository.findByPlaylistIdOrderBySortOrder(playlist.getId())) {
            existing.add(item.getSongId());
            order = Math.max(order, item.getSortOrder() + 1);
        }
        int maximum = Math.max(1, Math.min(limit, 500));
        for (Song song : songRepository.findAll()) {
            if (existing.size() >= maximum) break;
            if (!matches(song, tag) || existing.contains(song.getId())) continue;
            PlaylistSong item = new PlaylistSong();
            item.setPlaylistId(playlist.getId());
            item.setSongId(song.getId());
            item.setSortOrder(order++);
            item.setManual(false);
            playlistSongRepository.save(item);
            existing.add(song.getId());
        }
        return playlist;
    }

    private boolean matches(Song song, String tag) {
        return equalsAny(tag, song.getAiLanguage(), song.getAiEra(), song.getAiAgeRange(), song.getAiVocalForm())
                || contains(tag, song.getAiGenres()) || contains(tag, song.getAiThemes()) || contains(tag, song.getTags());
    }

    private boolean equalsAny(String tag, String... values) {
        return Arrays.stream(values).filter(Objects::nonNull).anyMatch(tag::equalsIgnoreCase);
    }

    private boolean contains(String tag, String[] values) {
        return values != null && Arrays.stream(values).anyMatch(tag::equalsIgnoreCase);
    }

    private AiAnalysisTask requireTask(Long id) { return taskRepository.findById(id).orElseThrow(() -> new ApiException("AI_TASK_NOT_FOUND", "AI 任务不存在")); }
    private Playlist requirePlaylist(Long id) { return playlistRepository.findById(id).orElseThrow(() -> new ApiException("PLAYLIST_NOT_FOUND", "歌单不存在")); }
    private void requireAiConfigured() {
        if (!properties.getAi().isEnabled()) throw new ApiException("AI_DISABLED", "AI 分析未启用");
        if (properties.getAi().getApiKey() == null || properties.getAi().getApiKey().isBlank()) throw new ApiException("AI_KEY_MISSING", "AI API Key 未配置");
    }
    private AiSongClassification parse(String json) {
        try { return objectMapper.readValue(json, AiSongClassification.class); }
        catch (JsonProcessingException e) { throw new ApiException("AI_RESULT_INVALID", "AI 结果无法解析"); }
    }
    private String write(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException("JSON 序列化失败", e); }
    }
}
