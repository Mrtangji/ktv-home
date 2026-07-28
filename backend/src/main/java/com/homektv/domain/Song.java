package com.homektv.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * 歌曲实体，对应 songs 表（详设§10）。
 */
@Entity
@Table(name = "songs")
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String artist = "未知歌手";

    @Column(name = "title_py", nullable = false)
    private String titlePy = "";

    @Column(name = "title_init", nullable = false)
    private String titleInit = "";

    @Column(name = "artist_py", nullable = false)
    private String artistPy = "";

    @Column(name = "artist_init", nullable = false)
    private String artistInit = "";

    @Column(nullable = false)
    private String language = "国语";

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "text[]", nullable = false)
    private String[] tags = new String[0];

    /** KTV_VIDEO / MV / AUDIO */
    @Column(name = "media_type", nullable = false)
    private String mediaType;

    @Column(name = "has_vocal_track", nullable = false)
    private boolean hasVocalTrack = false;

    @Column(name = "duration_ms", nullable = false)
    private int durationMs = 0;

    @Column(name = "cover_path")
    private String coverPath;

    @Column(name = "lyric_path")
    private String lyricPath;

    /** word / line / sub / none */
    @Column(name = "lyric_type", nullable = false)
    private String lyricType = "none";

    @Column(name = "play_count", nullable = false)
    private int playCount = 0;

    @Column(name = "ai_language")
    private String aiLanguage;

    @Column(name = "ai_era")
    private String aiEra;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "ai_genres", columnDefinition = "text[]", nullable = false)
    private String[] aiGenres = new String[0];

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "ai_themes", columnDefinition = "text[]", nullable = false)
    private String[] aiThemes = new String[0];

    @Column(name = "ai_age_range")
    private String aiAgeRange;

    @Column(name = "ai_vocal_form")
    private String aiVocalForm;

    @Column(name = "ai_analyzed_at")
    private OffsetDateTime aiAnalyzedAt;

    /** ok / file_missing */
    @Column(nullable = false)
    private String status = "ok";

    @Column(nullable = false, unique = true)
    private String fingerprint;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    // ---- getters / setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getTitlePy() { return titlePy; }
    public void setTitlePy(String titlePy) { this.titlePy = titlePy; }
    public String getTitleInit() { return titleInit; }
    public void setTitleInit(String titleInit) { this.titleInit = titleInit; }
    public String getArtistPy() { return artistPy; }
    public void setArtistPy(String artistPy) { this.artistPy = artistPy; }
    public String getArtistInit() { return artistInit; }
    public void setArtistInit(String artistInit) { this.artistInit = artistInit; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String[] getTags() { return tags; }
    public void setTags(String[] tags) { this.tags = tags; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public boolean isHasVocalTrack() { return hasVocalTrack; }
    public void setHasVocalTrack(boolean hasVocalTrack) { this.hasVocalTrack = hasVocalTrack; }
    public int getDurationMs() { return durationMs; }
    public void setDurationMs(int durationMs) { this.durationMs = durationMs; }
    public String getCoverPath() { return coverPath; }
    public void setCoverPath(String coverPath) { this.coverPath = coverPath; }
    public String getLyricPath() { return lyricPath; }
    public void setLyricPath(String lyricPath) { this.lyricPath = lyricPath; }
    public String getLyricType() { return lyricType; }
    public void setLyricType(String lyricType) { this.lyricType = lyricType; }
    public int getPlayCount() { return playCount; }
    public void setPlayCount(int playCount) { this.playCount = playCount; }
    public String getAiLanguage() { return aiLanguage; }
    public void setAiLanguage(String aiLanguage) { this.aiLanguage = aiLanguage; }
    public String getAiEra() { return aiEra; }
    public void setAiEra(String aiEra) { this.aiEra = aiEra; }
    public String[] getAiGenres() { return aiGenres; }
    public void setAiGenres(String[] aiGenres) { this.aiGenres = aiGenres; }
    public String[] getAiThemes() { return aiThemes; }
    public void setAiThemes(String[] aiThemes) { this.aiThemes = aiThemes; }
    public String getAiAgeRange() { return aiAgeRange; }
    public void setAiAgeRange(String aiAgeRange) { this.aiAgeRange = aiAgeRange; }
    public String getAiVocalForm() { return aiVocalForm; }
    public void setAiVocalForm(String aiVocalForm) { this.aiVocalForm = aiVocalForm; }
    public OffsetDateTime getAiAnalyzedAt() { return aiAnalyzedAt; }
    public void setAiAnalyzedAt(OffsetDateTime aiAnalyzedAt) { this.aiAnalyzedAt = aiAnalyzedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
