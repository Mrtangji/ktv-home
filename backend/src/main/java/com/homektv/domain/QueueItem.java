package com.homektv.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.OffsetDateTime;

/**
 * 播放队列项，对应 queue 表（详设§10）。
 * status: waiting / playing / done / skipped
 */
@Entity
@Table(name = "queue")
public class QueueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "song_id", nullable = false)
    private Long songId;

    @Column(name = "ordered_by")
    private Long orderedBy;

    /** 顶歌用分数插入，避免整列重排 */
    @Column(name = "order_index", nullable = false)
    private double orderIndex;

    @Column(nullable = false)
    private String status = "waiting";

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "played_at")
    private OffsetDateTime playedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSongId() { return songId; }
    public void setSongId(Long songId) { this.songId = songId; }
    public Long getOrderedBy() { return orderedBy; }
    public void setOrderedBy(Long orderedBy) { this.orderedBy = orderedBy; }
    public double getOrderIndex() { return orderIndex; }
    public void setOrderIndex(double orderIndex) { this.orderIndex = orderIndex; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getPlayedAt() { return playedAt; }
    public void setPlayedAt(OffsetDateTime playedAt) { this.playedAt = playedAt; }
}
