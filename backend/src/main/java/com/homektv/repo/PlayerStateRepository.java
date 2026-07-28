package com.homektv.repo;

import com.homektv.domain.PlayerState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PlayerStateRepository extends JpaRepository<PlayerState, Short> {

    /** 获取唯一的播放状态行（id=1） */
    default PlayerState getSingleton() {
        return findById(PlayerState.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("player_state 单行未初始化"));
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select state from PlayerState state where state.id = :id")
    Optional<PlayerState> findByIdForUpdate(Short id);

    default PlayerState getSingletonForUpdate() {
        return findByIdForUpdate(PlayerState.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("player_state 单行未初始化"));
    }
}
