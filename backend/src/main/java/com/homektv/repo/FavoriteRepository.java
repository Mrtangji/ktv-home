package com.homektv.repo;

import com.homektv.domain.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByUserIdAndSongId(Long userId, Long songId);
    long deleteByUserIdAndSongId(Long userId, Long songId);
}
