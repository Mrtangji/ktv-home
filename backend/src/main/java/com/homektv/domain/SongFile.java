package com.homektv.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * 歌曲文件源（一首歌可对应多个文件），对应 song_files 表（详设§10）。
 */
@Entity
@Table(name = "song_files")
public class SongFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "song_id", nullable = false)
    private Long songId;

    @Column(name = "file_path", nullable = false, unique = true)
    private String filePath;

    @Column(nullable = false)
    private String format;

    @Column(name = "audio_tracks", nullable = false)
    private int audioTracks = 1;

    /** 伴奏轨 index（0-based 音频序号，入库探测，免运行时猜轨），可空 */
    @Column(name = "vocal_track_index")
    private Integer vocalTrackIndex;

    /** 伴奏轨判定置信度：HIGH/MEDIUM/LOW/NONE，可空；LOW 供后台筛选人工复核 */
    @Column(name = "vocal_confidence")
    private String vocalConfidence;

    private String resolution;

    @Column(name = "file_size", nullable = false)
    private long fileSize = 0;

    @Column(name = "file_mtime", nullable = false)
    private OffsetDateTime fileMtime;

    @Column(nullable = false)
    private int priority = 0;
    @Column(nullable = false)
    private boolean valid = true;
    @Column(name = "file_role", nullable = false)
    private String fileRole = "LIBRARY";
    @Column(name = "source_path")
    private String sourcePath;
    @Column(name = "source_md5")
    private String sourceMd5;
    @Column(name = "output_md5")
    private String outputMd5;
    @Column(name = "transcode_required", nullable = false)
    private boolean transcodeRequired;
    @Column(name = "imported_at")
    private OffsetDateTime importedAt;
    @Column(name = "source_deleted", nullable = false)
    private boolean sourceDeleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSongId() { return songId; }
    public void setSongId(Long songId) { this.songId = songId; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public int getAudioTracks() { return audioTracks; }
    public void setAudioTracks(int audioTracks) { this.audioTracks = audioTracks; }
    public Integer getVocalTrackIndex() { return vocalTrackIndex; }
    public void setVocalTrackIndex(Integer vocalTrackIndex) { this.vocalTrackIndex = vocalTrackIndex; }
    public String getVocalConfidence() { return vocalConfidence; }
    public void setVocalConfidence(String vocalConfidence) { this.vocalConfidence = vocalConfidence; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public OffsetDateTime getFileMtime() { return fileMtime; }
    public void setFileMtime(OffsetDateTime fileMtime) { this.fileMtime = fileMtime; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public String getFileRole() { return fileRole; }
    public void setFileRole(String fileRole) { this.fileRole = fileRole; }
    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
    public String getSourceMd5() { return sourceMd5; }
    public void setSourceMd5(String sourceMd5) { this.sourceMd5 = sourceMd5; }
    public String getOutputMd5() { return outputMd5; }
    public void setOutputMd5(String outputMd5) { this.outputMd5 = outputMd5; }
    public boolean isTranscodeRequired() { return transcodeRequired; }
    public void setTranscodeRequired(boolean transcodeRequired) { this.transcodeRequired = transcodeRequired; }
    public OffsetDateTime getImportedAt() { return importedAt; }
    public void setImportedAt(OffsetDateTime importedAt) { this.importedAt = importedAt; }
    public boolean isSourceDeleted() { return sourceDeleted; }
    public void setSourceDeleted(boolean sourceDeleted) { this.sourceDeleted = sourceDeleted; }
}
