package com.homektv.library;

import com.homektv.config.AppProperties;
import com.homektv.domain.MediaImportRecord;
import com.homektv.domain.SongFile;
import com.homektv.media.FFprobeService;
import com.homektv.media.MediaProbe;
import com.homektv.repo.MediaImportRecordRepository;
import com.homektv.repo.SongFileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MediaImportServiceTest {

    @TempDir Path temp;
    private Path sourceDir;
    private Path targetDir;
    private FFprobeService probe;
    private MediaImportRecordRepository importRepo;
    private SongFileRepository songFileRepo;
    private LibraryScanService scanService;
    private MediaImportService service;
    private SettingService settingService;
    private MediaTranscoder mediaTranscoder;
    private final List<MediaImportRecord> saved = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        sourceDir = Files.createDirectories(temp.resolve("source"));
        targetDir = Files.createDirectories(temp.resolve("music"));
        AppProperties props = new AppProperties();
        props.setSourceLibraryPath(sourceDir.toString());
        props.setKtvLibraryPath(targetDir.toString());
        probe = mock(FFprobeService.class);
        importRepo = mock(MediaImportRecordRepository.class);
        songFileRepo = mock(SongFileRepository.class);
        scanService = mock(LibraryScanService.class);
        settingService = mock(SettingService.class);
        mediaTranscoder = mock(MediaTranscoder.class);
        when(settingService.transcodePolicy()).thenReturn(new SettingService.TranscodePolicy(
                List.of("mp4", "m4v", "mkv"), List.of("h264", "hevc"), List.of("aac", "mp3"),
                false, "mkv", "h264", "aac", false));
        when(importRepo.findBySourcePath(anyString())).thenReturn(Optional.empty());
        when(importRepo.save(any())).thenAnswer(invocation -> {
            MediaImportRecord record = invocation.getArgument(0);
            saved.add(record);
            return record;
        });
        when(scanService.ingestLibraryFile(any(), any(), anyString(), anyString(), anyBoolean()))
                .thenReturn(new LibraryScanService.IngestResult(true, 1L, 2L));
        service = new MediaImportService(props, probe, new FileHashService(), importRepo, songFileRepo,
                scanService, settingService, mediaTranscoder);
    }

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    @Test
    void scanAutomaticallyCopiesCompatibleVideo() throws Exception {
        Path source = sourceDir.resolve("周杰伦 - 晴天.mp4");
        Files.writeString(source, "compatible-media");
        when(probe.probe(source)).thenReturn(new MediaProbe(1000, 2, 0, true, "1920x1080",
                List.of(), "h264", "aac"));

        MediaImportService.SourceScanResult result = service.scanSourceLibrary();

        assertThat(result.copied()).isEqualTo(1);
        assertThat(result.pendingTranscode()).isZero();
        assertThat(Files.exists(targetDir.resolve(source.getFileName()))).isTrue();
        assertThat(saved.getLast().getAction()).isEqualTo(MediaImportService.COPIED);
        verify(scanService).ingestLibraryFile(any(), eq(source), anyString(), anyString(), eq(false));
    }

    @Test
    void scanLeavesIncompatibleVideoPending() throws Exception {
        Path source = sourceDir.resolve("薛之谦 - 绅士.mpg");
        Files.writeString(source, "legacy-media");
        when(probe.probe(source)).thenReturn(new MediaProbe(1000, 2, 0, true, "1920x1080",
                List.of(), "mpeg2video", "ac3"));

        MediaImportService.SourceScanResult result = service.scanSourceLibrary();

        assertThat(result.pendingTranscode()).isEqualTo(1);
        assertThat(result.copied()).isZero();
        assertThat(saved.getLast().getAction()).isEqualTo(MediaImportService.PENDING_TRANSCODE);
        try (Stream<Path> files = Files.list(targetDir)) {
            assertThat(files).isEmpty();
        }
        verifyNoInteractions(scanService);
    }

    @Test
    void scanMarksSourceMd5Duplicate() throws Exception {
        Path source = sourceDir.resolve("Beyond - 海阔天空.mp4");
        Files.writeString(source, "duplicate-media");
        when(probe.probe(source)).thenReturn(new MediaProbe(1000, 1, 0, true, "1920x1080",
                List.of(), "h264", "aac"));
        when(importRepo.existsBySourceMd5AndSourcePathNot(anyString(), eq(source.toString()))).thenReturn(true);

        MediaImportService.SourceScanResult result = service.scanSourceLibrary();

        assertThat(result.skippedSourceDuplicate()).isEqualTo(1);
        assertThat(saved.getLast().isDuplicateFlag()).isTrue();
        assertThat(saved.getLast().getAction()).isEqualTo(MediaImportService.SOURCE_DUPLICATE);
        verifyNoInteractions(scanService);
    }

    @Test
    void autoCleanupDeletesOnlySourcesWithVerifiedLibraryOutput() throws Exception {
        Path importedSource = sourceDir.resolve("歌手 - 已入库.mp4");
        Path importedOutput = targetDir.resolve("歌手 - 已入库.mp4");
        Path pendingSource = sourceDir.resolve("歌手 - 待转码.mpg");
        Files.writeString(importedSource, "source");
        Files.writeString(importedOutput, "output");
        Files.writeString(pendingSource, "pending");

        MediaImportRecord imported = new MediaImportRecord();
        imported.setId(10L);
        imported.setSourcePath(importedSource.toString());
        imported.setSourceFilename(importedSource.getFileName().toString());
        imported.setOutputPath(importedOutput.toString());
        imported.setSongFileId(20L);
        imported.setImportedFlag(true);
        MediaImportRecord pending = pendingRecord(11L, pendingSource.getFileName().toString());
        SongFile libraryFile = new SongFile();
        libraryFile.setId(20L);
        libraryFile.setFilePath(importedOutput.toString());
        libraryFile.setValid(true);
        when(importRepo.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(imported, pending));
        when(songFileRepo.findById(20L)).thenReturn(Optional.of(libraryFile));

        MediaImportService.AutoCleanupResult result = service.cleanupImportedSources();

        assertThat(result.scanned()).isEqualTo(2);
        assertThat(result.eligible()).isEqualTo(1);
        assertThat(result.deleted()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        assertThat(importedSource).doesNotExist();
        assertThat(importedOutput).exists();
        assertThat(pendingSource).exists();
        verify(importRepo).delete(imported);
    }

    @Test
    void priorityMovesQueuedRecordBehindCurrentTranscode() throws Exception {
        List<String> executionOrder = new ArrayList<>();
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        MediaImportRecord first = pendingRecord(1L, "歌手 - 第一首.mpg");
        MediaImportRecord second = pendingRecord(2L, "歌手 - 第二首.mpg");
        MediaImportRecord third = pendingRecord(3L, "歌手 - 第三首.mpg");
        Map<Long, MediaImportRecord> records = Map.of(1L, first, 2L, second, 3L, third);
        when(importRepo.findByIdIn(any())).thenReturn(List.of(first, second, third));
        when(importRepo.findById(anyLong())).thenAnswer(invocation -> Optional.ofNullable(records.get(invocation.getArgument(0))));
        when(probe.probe(any())).thenReturn(new MediaProbe(1000, 1, 0, true, "1920x1080",
                List.of(), "mpeg2video", "ac3"));
        when(mediaTranscoder.transcode(any(), any(), any(), eq(true))).thenAnswer(invocation -> {
            Path source = invocation.getArgument(0);
            Path output = invocation.getArgument(1);
            executionOrder.add(source.getFileName().toString());
            if (executionOrder.size() == 1) {
                firstStarted.countDown();
                assertThat(releaseFirst.await(5, TimeUnit.SECONDS)).isTrue();
            }
            Files.writeString(output, "output-" + executionOrder.size());
            return output;
        });

        service.startPendingTranscode(List.of(1L, 2L, 3L), false);
        assertThat(firstStarted.await(5, TimeUnit.SECONDS)).isTrue();
        MediaImportService.PriorityResult priority = service.prioritizeTranscode(3L);
        assertThat(priority.status()).isEqualTo("MOVED");
        assertThat(priority.progress().total()).isEqualTo(3);
        assertThat(priority.progress().priorityRecordIds()).containsExactly(3L);
        releaseFirst.countDown();
        awaitFinished();

        assertThat(executionOrder).containsExactly(
                "歌手 - 第一首.mpg", "歌手 - 第三首.mpg", "歌手 - 第二首.mpg");
        assertThat(service.getProgress().completed()).isEqualTo(3);
    }

    private MediaImportRecord pendingRecord(Long id, String filename) throws Exception {
        Path source = sourceDir.resolve(filename);
        Files.writeString(source, "source-" + id);
        MediaImportRecord record = new MediaImportRecord();
        record.setId(id);
        record.setSourcePath(source.toString());
        record.setSourceFilename(filename);
        record.setSourceMd5(new FileHashService().md5(source));
        record.setAction(MediaImportService.PENDING_TRANSCODE);
        record.setTranscodeRequired(true);
        return record;
    }

    private void awaitFinished() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (service.getProgress().running() && System.nanoTime() < deadline) Thread.sleep(20);
        assertThat(service.getProgress().running()).isFalse();
    }
}
