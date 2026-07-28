package com.homektv.web;

import com.homektv.library.AdminService;
import com.homektv.library.LibraryScanService;
import com.homektv.library.LibraryWatchService;
import com.homektv.library.MediaImportService;
import com.homektv.library.SettingService;
import com.homektv.library.TranscodeService;
import com.homektv.library.TranscodeHardwareService;
import com.homektv.library.SongReparseService;
import com.homektv.domain.Song;
import com.homektv.domain.MediaImportRecord;
import com.homektv.web.dto.DashboardDto;
import com.homektv.web.dto.AdminSongDto;
import com.homektv.web.dto.MediaImportRecordDto;
import com.homektv.web.dto.SongDto;
import com.homektv.web.dto.SongEditRequest;
import com.homektv.web.dto.VocalReviewDto;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理后台 API（P2.1-P2.6/P1.8，详设§8/§11.1）。局域网免登录。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminScanController {

    private final LibraryScanService scanService;
    private final LibraryWatchService libraryWatchService;
    private final AdminService adminService;
    private final MediaImportService mediaImportService;
    private final SettingService settingService;
    private final TranscodeService transcodeService;
    private final TranscodeHardwareService transcodeHardwareService;
    private final SongReparseService reparseService;

    public AdminScanController(LibraryScanService scanService, LibraryWatchService libraryWatchService,
                               AdminService adminService,
                               MediaImportService mediaImportService,
                               SettingService settingService, TranscodeService transcodeService,
                               TranscodeHardwareService transcodeHardwareService, SongReparseService reparseService) {
        this.scanService = scanService;
        this.libraryWatchService = libraryWatchService;
        this.adminService = adminService;
        this.mediaImportService = mediaImportService;
        this.settingService = settingService;
        this.transcodeService = transcodeService;
        this.transcodeHardwareService = transcodeHardwareService;
        this.reparseService = reparseService;
    }

    /** 触发全量/增量扫描（P1.8） */
    @PostMapping("/scan")
    public Map<String, Object> scan() {
        MediaImportService.SourceScanResult sourceScan = mediaImportService.scanSourceLibrary();
        return Map.of("sourceScan", sourceScan);
    }

    @PostMapping("/scan/start")
    public MediaImportService.SourceScanProgress startScan() {
        return mediaImportService.startSourceScan();
    }

    @GetMapping("/scan/progress")
    public MediaImportService.SourceScanProgress scanProgress() {
        return mediaImportService.getScanProgress();
    }

    @GetMapping("/source-library")
    public Map<String, Object> sourceLibrary(@RequestParam(defaultValue = "") String keyword,
                                             @RequestParam(defaultValue = "") String status,
                                             @RequestParam(defaultValue = "") String formatAnalysis,
                                             @RequestParam(required = false) Boolean sourceDeleted,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        Page<MediaImportRecord> records = mediaImportService.listSourceLibrary(
                keyword, status, formatAnalysis, sourceDeleted, page, size);
        return Map.of(
                "content", records.getContent().stream().map(MediaImportRecordDto::from).toList(),
                "total", records.getTotalElements(),
                "page", records.getNumber(),
                "totalPages", records.getTotalPages()
        );
    }

    @PostMapping("/source-library/transcode")
    public MediaImportService.TranscodeProgress transcodeSourceLibrary(
            @RequestBody(required = false) SourceLibraryRequest request) {
        return mediaImportService.startPendingTranscode(
                request == null ? List.of() : request.ids(),
                request != null && request.all());
    }

    @GetMapping("/source-library/progress")
    public MediaImportService.TranscodeProgress sourceLibraryProgress() {
        return mediaImportService.getProgress();
    }

    @PostMapping("/source-library/transcode/priority")
    public MediaImportService.PriorityResult prioritizeSourceTranscode(@RequestBody PriorityTranscodeRequest request) {
        return mediaImportService.prioritizeTranscode(request.id());
    }

    @PostMapping("/source-library/cleanup")
    public MediaImportService.AutoCleanupResult cleanupImportedSources() {
        return mediaImportService.cleanupImportedSources();
    }

    @DeleteMapping("/source-library")
    public MediaImportService.DeleteSourcesResult deleteSources(@RequestBody SourceLibraryRequest request) {
        if (!request.ids().isEmpty()) {
            return mediaImportService.deleteSources(request.ids());
        }
        return mediaImportService.deleteSourcesByFilter(
                request.keyword(), request.status(), request.formatAnalysis(), request.sourceDeleted());
    }

    @GetMapping("/imports")
    public Map<String, Object> imports(@RequestParam(defaultValue = "") String action,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        org.springframework.data.domain.Page<MediaImportRecord> p = mediaImportService.listRecords(action, page, size);
        return Map.of(
                "content", p.getContent().stream().map(MediaImportRecordDto::from).toList(),
                "total", p.getTotalElements(),
                "page", p.getNumber(),
                "totalPages", p.getTotalPages()
        );
    }

    @DeleteMapping("/imports/{id}/source")
    public Map<String, Object> deleteSource(@PathVariable Long id) {
        mediaImportService.deleteSource(id);
        return Map.of("status", "deleted", "id", id);
    }

    /** Generate an Android TV compatible H.264/AAC derivative; original remains untouched. */
    @PostMapping("/songs/{id}/transcode")
    public Map<String, Object> transcode(@PathVariable Long id) {
        TranscodeService.Result result = transcodeService.transcodeSong(id);
        LibraryScanService.ScanResult scan = scanService.scanAll();
        return Map.of("status", "completed", "sourceFileId", result.sourceFileId(),
                "outputPath", result.outputPath(), "outputBytes", result.outputBytes(),
                "scan", scan);
    }

    /** 仪表盘（P2.1） */
    @GetMapping("/status")
    public DashboardDto status() {
        return adminService.dashboard();
    }

    /** 曲库分页列表 + 类型筛选（P2.2） */
    @GetMapping("/songs")
    public Map<String, Object> listSongs(@RequestParam(defaultValue = "") String keyword,
                                         @RequestParam(defaultValue = "") String type,
                                         @RequestParam(defaultValue = "") String source,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        Page<AdminSongDto> p = adminService.listAdminSongs(keyword, type, source, page, size);
        return Map.of(
                "content", p.getContent(),
                "total", p.getTotalElements(),
                "page", p.getNumber(),
                "totalPages", p.getTotalPages()
        );
    }

    /** 编辑曲目（P2.3） */
    @PutMapping("/songs/{id}")
    public SongDto editSong(@PathVariable Long id, @RequestBody SongEditRequest req) {
        return SongDto.from(adminService.editSong(id, req));
    }

    /** 删除记录（P2.5，不删 NAS 文件） */
    @DeleteMapping("/songs/{id}")
    public Map<String, String> deleteSong(@PathVariable Long id) {
        adminService.deleteSong(id);
        return Map.of("status", "deleted");
    }

    @DeleteMapping("/songs")
    public Map<String, Object> deleteSongs(@RequestBody SongIdsRequest request) {
        return Map.of("status", "deleted", "deleted", adminService.deleteSongs(request.ids()));
    }

    /** 伴奏轨低置信度复核列表：入库判不准原伴唱、需人工核对的文件源 */
    @GetMapping("/vocal-review")
    public Map<String, Object> vocalReview(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        Page<VocalReviewDto> p = adminService.listVocalReview(page, size);
        return Map.of(
                "content", p.getContent(),
                "total", p.getTotalElements(),
                "page", p.getNumber(),
                "totalPages", p.getTotalPages()
        );
    }

    /** 人工确认伴奏轨 index（0-based），标为高置信度、脱离复核列表 */
    @PutMapping("/files/{fileId}/vocal-track")
    public Map<String, Object> confirmVocalTrack(@PathVariable Long fileId,
                                                 @RequestBody Map<String, Object> body) {
        int index = ((Number) body.getOrDefault("accompanimentIndex", 1)).intValue();
        adminService.confirmVocalTrack(fileId, index);
        return Map.of("status", "confirmed", "fileId", fileId, "accompanimentIndex", index);
    }

    /** 读取设置（P2.6） */
    @GetMapping("/settings")
    public Map<String, Object> getSettings() {
        return settingService.getAll();
    }

    /** 写入设置（P2.6） */
    @PutMapping("/settings")
    public Map<String, Object> putSettings(@RequestBody Map<String, Object> settings) {
        if (Boolean.TRUE.equals(settings.get("transcode_hardware_acceleration"))) {
            String codec = String.valueOf(settings.getOrDefault("transcode_video_codec",
                    settingService.transcodePolicy().videoCodec())).toLowerCase();
            transcodeHardwareService.requireAvailable(codec);
        }
        settingService.putAll(settings);
        libraryWatchService.reloadFromSettings();
        return settingService.getAll();
    }

    @GetMapping("/settings/transcode-hardware")
    public TranscodeHardwareService.HardwareStatus transcodeHardware() {
        return transcodeHardwareService.detect();
    }

    @PostMapping("/settings/transcode-defaults")
    public Map<String, Object> resetTranscodeDefaults() {
        return settingService.resetTranscodeDefaults();
    }

    @PostMapping("/songs/reparse/preview")
    public List<SongReparseService.Preview> previewReparse(@RequestBody ReparseRequest request) {
        return reparseService.preview(request.songIds(), request.rule());
    }

    @PostMapping("/songs/reparse/apply")
    public SongReparseService.ApplyResult applyReparse(@RequestBody ReparseRequest request) {
        return reparseService.apply(request.songIds(), request.rule());
    }

    public record ReparseRequest(List<Long> songIds, String rule) {}
    public record PriorityTranscodeRequest(Long id) {}
    public record SongIdsRequest(List<Long> ids) {
        public SongIdsRequest {
            ids = ids == null ? List.of() : ids;
        }
    }
    public record SourceLibraryRequest(List<Long> ids, boolean all, String keyword, String status,
                                       String formatAnalysis, Boolean sourceDeleted) {
        public SourceLibraryRequest {
            ids = ids == null ? List.of() : ids;
        }
    }
}
