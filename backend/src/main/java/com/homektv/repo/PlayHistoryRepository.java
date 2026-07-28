package com.homektv.repo;

import com.homektv.domain.PlayHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface PlayHistoryRepository extends JpaRepository<PlayHistory, Long> {

    /** 点唱排行：按时间窗口统计各歌播放次数，返回 [songId, cnt] 降序（P3.4） */
    @Query(value = """
            SELECT song_id, COUNT(*) AS cnt
            FROM play_history
            WHERE played_at >= :since
            GROUP BY song_id
            ORDER BY cnt DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> ranking(@Param("since") OffsetDateTime since, @Param("limit") int limit);

    /** 最近播放历史（今晚已唱），按时间倒序 */
    List<PlayHistory> findTop50ByOrderByPlayedAtDesc();

    void deleteBySongId(Long songId);
}
