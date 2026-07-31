package com.homektv.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homektv.domain.AiAnalysisTask;
import com.homektv.domain.Playlist;
import com.homektv.domain.PlaylistSong;
import com.homektv.domain.Song;
import com.homektv.repo.AiAnalysisTaskRepository;
import com.homektv.repo.PlaylistRepository;
import com.homektv.repo.PlaylistSongRepository;
import com.homektv.repo.SongRepository;
import com.homektv.repo.MediaImportRecordRepository;
import com.homektv.domain.MediaImportRecord;
import com.homektv.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.homektv.library.AssetWriter;

import java.io.IOException;

import java.util.*;

/**
 * AI 歌库服务 —— 管理 AI 分析任务、AI 歌单生成与分类结果应用。
 *
 * AI library service — manages AI analysis tasks, AI-generated playlists, and classification result application.
 */
@Service
public class AiLibraryService {
    private static final List<String> ACTIVE_STATUSES = List.of("pending", "processing", "review");

    private final AiAnalysisTaskRepository taskRepository;
    private final SongRepository songRepository;
    private final PlaylistRepository playlistRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final AiAnalysisWorker worker;
    private final ObjectMapper objectMapper;
    private final AiConfigService configService;
    private final AssetWriter assetWriter;
    private final AiClassificationApplier classificationApplier;
    private final OpenAiCompatibleClient aiClient;
    private final MediaImportRecordRepository importRecordRepository;

    public AiLibraryService(AiAnalysisTaskRepository taskRepository, SongRepository songRepository,
                            PlaylistRepository playlistRepository, PlaylistSongRepository playlistSongRepository,
                            AiAnalysisWorker worker, ObjectMapper objectMapper, AiConfigService configService,
                            AssetWriter assetWriter, AiClassificationApplier classificationApplier,
                            OpenAiCompatibleClient aiClient, MediaImportRecordRepository importRecordRepository) {
        this.taskRepository = taskRepository;
        this.songRepository = songRepository;
        this.playlistRepository = playlistRepository;
        this.playlistSongRepository = playlistSongRepository;
        this.worker = worker;
        this.objectMapper = objectMapper;
        this.configService = configService;
        this.assetWriter = assetWriter;
        this.classificationApplier = classificationApplier;
        this.aiClient = aiClient;
        this.importRecordRepository = importRecordRepository;
    }

    /**
     * 为指定歌曲创建 AI 分析任务；若该歌曲已有进行中的任务则抛出异常。
     *
     * Creates an AI analysis task for the given song; throws if an active task already exists.
     *
     * @param songId 歌曲 ID / song ID
     * @return 创建的 AI 分析任务 / the created AI analysis task
     */
    public AiAnalysisTask createTask(Long songId) {
        requireAiConfigured();
        songRepository.findById(songId).orElseThrow(() -> new ApiException("SONG_NOT_FOUND", "歌曲不存在"));
        if (taskRepository.existsBySongIdAndStatusIn(songId, ACTIVE_STATUSES)) {
            throw new ApiException("AI_TASK_EXISTS", "该歌曲已有待处理的 AI 任务");
        }
        AiAnalysisTask task = new AiAnalysisTask();
        task.setSongId(songId);
        AiConfigService.ResolvedConfig config = configService.resolve();
        task.setModel(config.bulkModel());
        task.setModelRole("BULK");
        task.setTargetType("SONG");
        task.setTargetId(songId);
        try {
            task = taskRepository.saveAndFlush(task);
        } catch (DataIntegrityViolationException conflict) {
            if (taskRepository.findFirstBySongIdAndStatusInOrderByCreatedAtDesc(songId, ACTIVE_STATUSES).isPresent()) {
                throw new ApiException("AI_TASK_EXISTS", "该歌曲已有待处理的 AI 任务");
            }
            throw conflict;
        }
        worker.analyze(task.getId());
        return task;
    }

    public AiAnalysisTask createImportTask(Long recordId) {
        requireAiConfigured();
        importRecordRepository.findById(recordId)
                .orElseThrow(() -> new ApiException("IMPORT_RECORD_NOT_FOUND", "导入记录不存在"));
        if (taskRepository.existsByTargetTypeAndTargetIdAndStatusIn("IMPORT_RECORD", recordId, ACTIVE_STATUSES))
            throw new ApiException("AI_TASK_EXISTS", "该导入记录已有待处理的 AI 任务");
        AiConfigService.ResolvedConfig config = configService.resolve();
        AiAnalysisTask task = new AiAnalysisTask();
        task.setTargetType("IMPORT_RECORD");
        task.setTargetId(recordId);
        task.setSongId(null);
        task.setModel(config.bulkModel());
        task.setModelRole("BULK");
        task = taskRepository.saveAndFlush(task);
        worker.analyze(task.getId());
        return task;
    }

    /**
     * 为所有尚未分析的歌曲批量创建 AI 分类任务，上限受参数控制（最大 500）。
     *
     * Batch-creates AI classification tasks for all unanalyzed songs, capped by the parameter (max 500).
     *
     * @param limit 任务数量上限 / maximum number of tasks
     * @return 创建的任务列表 / list of created tasks
     */
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

    /** Create a repair batch for every existing song, including already analyzed songs. */
    public Map<String, Object> createRepairBatch() {
        configService.requireConfigured();
        String batchId = UUID.randomUUID().toString();
        AiConfigService.ResolvedConfig config = configService.resolve();
        int created = 0;
        for (Song song : songRepository.findAll()) {
            AiAnalysisTask task = new AiAnalysisTask();
            task.setSongId(song.getId());
            task.setTargetId(song.getId());
            task.setTargetType("SONG");
            task.setBatchId(batchId);
            task.setModel(config.bulkModel());
            task.setModelRole("BULK");
            taskRepository.saveAndFlush(task);
            worker.analyze(task.getId());
            created++;
        }
        return Map.of("batchId", batchId, "created", created);
    }

    @Transactional
    public Map<String, Object> pauseRepairBatch(String batchId) {
        List<AiAnalysisTask> tasks = taskRepository.findByBatchIdOrderByCreatedAtAsc(batchId);
        if (tasks.isEmpty()) throw new ApiException("AI_BATCH_NOT_FOUND", "修复批次不存在");
        int changed = 0;
        for (AiAnalysisTask task : tasks) {
            if (Set.of("pending", "processing").contains(task.getStatus())) {
                task.setStatus("paused");
                changed++;
            }
        }
        taskRepository.saveAll(tasks);
        return repairProgress(batchId);
    }

    @Transactional
    public Map<String, Object> resumeRepairBatch(String batchId) {
        configService.requireConfigured();
        List<AiAnalysisTask> tasks = taskRepository.findByBatchIdOrderByCreatedAtAsc(batchId);
        if (tasks.isEmpty()) throw new ApiException("AI_BATCH_NOT_FOUND", "修复批次不存在");
        int resumed = 0;
        for (AiAnalysisTask task : tasks) {
            if ("paused".equals(task.getStatus())) {
                task.setStatus("pending");
                taskRepository.saveAndFlush(task);
                worker.analyze(task.getId());
                resumed++;
            }
        }
        return Map.of("batchId", batchId, "resumed", resumed);
    }

    @Transactional
    public Map<String, Object> retryFailedRepairBatch(String batchId) {
        configService.requireConfigured();
        List<AiAnalysisTask> tasks = taskRepository.findByBatchIdOrderByCreatedAtAsc(batchId);
        if (tasks.isEmpty()) throw new ApiException("AI_BATCH_NOT_FOUND", "修复批次不存在");
        int retried = 0;
        for (AiAnalysisTask task : tasks) {
            if ("failed".equals(task.getStatus())) {
                task.setStatus("pending");
                taskRepository.saveAndFlush(task);
                worker.analyze(task.getId());
                retried++;
            }
        }
        return Map.of("batchId", batchId, "retried", retried);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> repairProgress(String batchId) {
        List<AiAnalysisTask> tasks = taskRepository.findByBatchIdOrderByCreatedAtAsc(batchId);
        long completed = tasks.stream().filter(t -> Set.of("auto_applied", "review", "failed", "applied").contains(t.getStatus())).count();
        return Map.of("batchId", batchId, "total", tasks.size(), "completed", completed,
                "failed", tasks.stream().filter(t -> "failed".equals(t.getStatus())).count(),
                "review", tasks.stream().filter(t -> "review".equals(t.getStatus())).count(),
                "paused", tasks.stream().filter(t -> "paused".equals(t.getStatus())).count(),
                "running", tasks.stream().anyMatch(t -> Set.of("pending", "processing").contains(t.getStatus())));
    }

    /** Preview an AI-curated playlist. No playlist or playlist-song rows are written here. */
    public Map<String, Object> previewPlaylist(String instruction, int limit) {
        configService.requireConfigured();
        if (instruction == null || instruction.isBlank()) throw new ApiException("INVALID_ARGUMENT", "请描述想要的歌单");
        int maximum = Math.max(1, Math.min(limit, 500));
        List<Song> all = songRepository.findAll();
        List<Song> candidates = all.size() > 5000 ? all.stream()
                .sorted(Comparator.comparingInt(Song::getPlayCount).reversed()
                        .thenComparing(s -> s.getAiAnalyzedAt() == null)).limit(5000).toList() : all;
        StringBuilder candidateJson = new StringBuilder("[");
        for (Song song : candidates) {
            if (candidateJson.length() > 1) candidateJson.append(',');
            candidateJson.append("{\"id\":").append(song.getId()).append(",\"title\":")
                    .append(json(song.getTitle())).append(",\"artist\":").append(json(song.getArtist()))
                    .append(",\"language\":").append(json(song.getLanguage())).append(",\"tags\":")
                    .append(json(String.join("、", song.getTags()))).append("}");
        }
        candidateJson.append(']');
        JsonNode intent = aiClient.completeJson("BULK",
                "你是家庭 KTV 歌单需求分析器。只返回 JSON，不要 Markdown。输出 intent（自然语言策划意图）、languages(string[]), genres(string[]), themes(string[]), eras(string[]), artists(string[]), mood(string), diversity(string), maxSongs(number)。不要选择歌曲，不要编造歌曲 ID。",
                "用户策划要求：" + instruction + "\n最多歌曲：" + maximum, 900);
        String selectionPrompt = "策划意图：" + intent + "\n最多选择 " + maximum
                + " 首。只能从候选中返回 ID，按播放顺序排列；候选外、重复或无效 ID 都不要返回。候选：" + candidateJson;
        String selectionRole = configService.resolve().reasoningModel().isBlank() ? "BULK" : "REASONING";
        JsonNodeResult result = new JsonNodeResult(aiClient.completeJson(selectionRole,
                "你是家庭 KTV 歌单策划器。根据结构化策划意图从候选曲库中选歌。只返回 JSON：name(string), description(string), songIds(number[]), reasons(string[]), confidence(number)。songIds 只能来自候选，去重，不超过上限。",
                selectionPrompt, 2600));
        Set<Long> allowed = candidates.stream().map(Song::getId).collect(java.util.stream.Collectors.toSet());
        List<Long> ids = new ArrayList<>();
        for (var node : result.node().path("songIds")) {
            long id = node.asLong(-1);
            if (allowed.contains(id) && !ids.contains(id) && ids.size() < maximum) ids.add(id);
        }
        return Map.of("name", result.node().path("name").asText("AI 歌单"),
                "description", result.node().path("description").asText(""), "songIds", ids,
                "reasons", result.node().path("reasons"), "intent", intent,
                "candidateCount", candidates.size());
    }

    private String json(String value) {
        try { return objectMapper.writeValueAsString(value == null ? "" : value); }
        catch (JsonProcessingException e) { return "\"\""; }
    }

    private record JsonNodeResult(com.fasterxml.jackson.databind.JsonNode node) { }

    /**
     * 列出最近的 AI 分析任务（最新 100 条）。
     *
     * Lists the most recent AI analysis tasks (up to 100).
     *
     * @return AI 分析任务列表 / list of AI analysis tasks
     */
    public List<AiAnalysisTask> listTasks() {
        return taskRepository.findTop100ByOrderByCreatedAtDesc();
    }

    /**
     * 列出所有歌单，返回含歌曲数量、手动数量等摘要信息的列表。
     *
     * Lists all playlists with summary info including song count and manual count.
     *
     * @return 歌单摘要列表 / list of playlist summaries
     */
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

    /**
     * 获取歌单详情，包含歌曲列表及每首歌的标题、歌手等信息。
     *
     * Gets playlist details including the song list with title, artist, etc.
     *
     * @param playlistId 歌单 ID / playlist ID
     * @return 歌单详情 map / playlist detail map
     */
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

    /**
     * 创建或更新歌单；名称为空时抛出异常。
     *
     * Creates or updates a playlist; throws when the name is blank.
     *
     * @param playlistId 歌单 ID，为 null 时新建 / playlist ID, create new when null
     * @param name 歌单名称 / playlist name
     * @param description 描述 / description
     * @param theme 主题标签 / theme tag
     * @param publicVisible 是否公开可见 / whether publicly visible
     * @return 保存后的歌单实体 / the saved playlist entity
     */
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

    /**
     * 删除指定歌单。
     *
     * Deletes the specified playlist.
     *
     * @param playlistId 歌单 ID / playlist ID
     */
    @Transactional
    public void deletePlaylist(Long playlistId) {
        playlistRepository.delete(requirePlaylist(playlistId));
    }

    /**
     * 向歌单中添加一首歌曲；已存在的歌曲会被忽略。
     *
     * Adds a song to the playlist; silently ignored if the song is already present.
     *
     * @param playlistId 歌单 ID / playlist ID
     * @param songId 歌曲 ID / song ID
     * @return 更新后的歌单详情 / updated playlist detail
     */
    @Transactional
    public Map<String, Object> addPlaylistSong(Long playlistId, Long songId) {
        requirePlaylist(playlistId);
        Song song = songRepository.findById(songId).orElseThrow(() -> new ApiException("SONG_NOT_FOUND", "歌曲不存在"));
        playlistSongRepository.lockPlaylist(playlistId);
        List<PlaylistSong> current = playlistSongRepository.findByPlaylistIdOrderBySortOrder(playlistId);
        if (current.stream().anyMatch(item -> item.getSongId().equals(songId))) return playlistDetail(playlistId);
        int sortOrder = current.stream().mapToInt(PlaylistSong::getSortOrder).max().orElse(-1) + 1;
        playlistSongRepository.insertManualIfAbsent(playlistId, song.getId(), sortOrder);
        return playlistDetail(playlistId);
    }

    /**
     * 从歌单中移除指定歌曲。
     *
     * Removes the specified song from the playlist.
     *
     * @param playlistId 歌单 ID / playlist ID
     * @param songId 歌曲 ID / song ID
     */
    @Transactional
    public void removePlaylistSong(Long playlistId, Long songId) {
        requirePlaylist(playlistId);
        playlistSongRepository.deleteByPlaylistIdAndSongId(playlistId, songId);
    }

    /**
     * 上传歌单封面图片（仅支持 JPG、PNG、WebP，最大 5MB）。
     *
     * Uploads a cover image for the playlist (JPG/PNG/WebP only, max 5MB).
     *
     * @param playlistId 歌单 ID / playlist ID
     * @param file 上传的图片文件 / uploaded image file
     * @return 更新后的歌单实体 / updated playlist entity
     */
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

    /**
     * 重新排列歌单中歌曲的顺序。
     *
     * Reorders the songs within the playlist.
     *
     * @param playlistId 歌单 ID / playlist ID
     * @param songIds   按新顺序排列的歌曲 ID 列表 / ordered song ID list
     * @return 更新后的歌单详情 / updated playlist detail
     */
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

    /**
     * 重新执行失败的 AI 分析任务。
     *
     * Retries a failed AI analysis task.
     *
     * @param taskId 任务 ID / task ID
     * @return 重置后的任务 / the reset task
     */
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

    /**
     * 应用 AI 分类结果到歌曲，可传入覆盖值。
     *
     * Applies AI classification results to the song, with optional override.
     *
     * @param taskId 任务 ID / task ID
     * @param override 覆盖的分类结果，为 null 时使用任务中的结果 / override classification, uses task result when null
     * @return 更新后的歌曲实体 / updated song entity
     */
    @Transactional
    public Object apply(Long taskId, AiSongClassification override) {
        AiAnalysisTask task = requireTask(taskId);
        if (!"review".equals(task.getStatus())) throw new ApiException("INVALID_AI_TASK", "任务不处于待确认状态");
        AiSongClassification result = override == null ? parse(task.getResultJson()) : override;
        Object target;
        if ("IMPORT_RECORD".equals(task.getTargetType())) {
            MediaImportRecord record = importRecordRepository.findById(task.getTargetId())
                    .orElseThrow(() -> new ApiException("IMPORT_RECORD_NOT_FOUND", "导入记录不存在"));
            if (result.title() == null || result.title().isBlank())
                throw new ApiException("AI_RESULT_INVALID", "歌名不能为空");
            record.setParsedTitle(result.title().trim());
            record.setParsedArtist(result.artist() == null ? "" : result.artist().trim());
            record.setReason("人工确认 AI 文件身份：" + (result.reason() == null ? "" : result.reason()));
            target = importRecordRepository.save(record);
        } else {
            target = classificationApplier.apply(task.getSongId(), result);
        }
        task.setStatus("applied");
        task.setResultJson(write(result));
        taskRepository.save(task);
        return target;
    }

    /**
     * 根据 AI 标签自动生成歌单，将匹配标签的歌曲填入，并保留手动添加的曲目。
     *
     * Generates a playlist by AI tag — fills in matching songs while preserving manually added ones.
     *
     * @param name 歌单名称 / playlist name
     * @param tag  AI 分类标签 / AI classification tag
     * @param limit 匹配歌曲数量上限 / max matching songs
     * @return 生成/更新后的歌单实体 / the generated or updated playlist entity
     */
    @Transactional
    public Playlist generatePlaylist(String name, String tag, int limit) {
        if (name == null || name.isBlank() || tag == null || tag.isBlank()) {
            throw new ApiException("INVALID_ARGUMENT", "歌单名称和 AI 标签不能为空");
        }
        String normalizedName = name.trim();
        playlistRepository.lockGeneratedName(normalizedName);
        Playlist playlist = playlistRepository.findByName(normalizedName).orElseGet(Playlist::new);
        playlist.setName(normalizedName);
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

    // 判断歌曲是否匹配指定 AI 标签 / Checks whether a song matches the given AI tag
    private boolean matches(Song song, String tag) {
        return equalsAny(tag, song.getAiLanguage(), song.getAiEra(), song.getAiAgeRange(), song.getAiVocalForm())
                || contains(tag, song.getAiGenres()) || contains(tag, song.getAiThemes()) || contains(tag, song.getTags());
    }

    // 忽略大小写判断 tag 是否等于任一给定值 / Case-insensitive check if tag equals any of the given values
    private boolean equalsAny(String tag, String... values) {
        return Arrays.stream(values).filter(Objects::nonNull).anyMatch(tag::equalsIgnoreCase);
    }

    // 忽略大小写判断 tag 是否存在于数组中 / Case-insensitive check if tag exists in array
    private boolean contains(String tag, String[] values) {
        return values != null && Arrays.stream(values).anyMatch(tag::equalsIgnoreCase);
    }

    // 查找任务，不存在则抛异常 / Find task by ID or throw
    private AiAnalysisTask requireTask(Long id) { return taskRepository.findById(id).orElseThrow(() -> new ApiException("AI_TASK_NOT_FOUND", "AI 任务不存在")); }
    // 查找歌单，不存在则抛异常 / Find playlist by ID or throw
    private Playlist requirePlaylist(Long id) { return playlistRepository.findById(id).orElseThrow(() -> new ApiException("PLAYLIST_NOT_FOUND", "歌单不存在")); }
    // 校验 AI 配置是否已启用且 API Key 已设置 / Verify AI is enabled and API key is configured
    private void requireAiConfigured() {
        configService.requireConfigured();
    }
    // 将 JSON 字符串解析为分类结果对象 / Parse JSON string to classification object
    private AiSongClassification parse(String json) {
        try { return objectMapper.readValue(json, AiSongClassification.class); }
        catch (JsonProcessingException e) { throw new ApiException("AI_RESULT_INVALID", "AI 结果无法解析"); }
    }
    // 将对象序列化为 JSON 字符串 / Serialize object to JSON string
    private String write(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException("JSON 序列化失败", e); }
    }
}
