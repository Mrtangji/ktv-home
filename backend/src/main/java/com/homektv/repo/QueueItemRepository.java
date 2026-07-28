package com.homektv.repo;

import com.homektv.domain.QueueItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface QueueItemRepository extends JpaRepository<QueueItem, Long> {

    /** 指定状态的队列，按 order_index 升序 */
    List<QueueItem> findByStatusOrderByOrderIndexAsc(String status);

    List<QueueItem> findByStatusInOrderByOrderIndexAsc(List<String> statuses);

    /** 某歌是否已在等待队列中 */
    Optional<QueueItem> findFirstBySongIdAndStatus(Long songId, String status);

    /** 等待队列中最大 order_index（用于追加队尾） */
    Optional<QueueItem> findFirstByStatusOrderByOrderIndexDesc(String status);

    long countByStatus(String status);

    List<QueueItem> findBySongId(Long songId);

    @Query(value = "SELECT pg_advisory_xact_lock(:songId)", nativeQuery = true)
    void lockSongForOrder(Long songId);
}
