package com.homektv.repo;

import com.homektv.domain.Song;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 综合搜索查询（P1.6，详设§10）。
 * 支持中文/全拼/首字母混合，必须覆盖 title/artist 的拼音与首字母四类条件，
 * 否则「zjl→周杰伦」（歌手首字母）会失效。
 * 排序：完全匹配 > 前缀 > 模糊，KTV 版优先，再按点唱量。
 */
public interface SongSearchRepository extends JpaRepository<Song, Long> {

    @Query(value = """
            SELECT * FROM songs
            WHERE status = 'ok' AND (
                  title ILIKE '%' || :kw || '%'
               OR artist ILIKE '%' || :kw || '%'
               OR title_init = :kw
               OR title_py LIKE :kw || '%'
               OR artist_init = :kw
               OR artist_py LIKE :kw || '%'
            )
            ORDER BY
              (title = :kw) DESC,
              (artist = :kw) DESC,
              (title ILIKE :kw || '%') DESC,
              (media_type = 'KTV_VIDEO') DESC,
              similarity(title, :kw) DESC,
              play_count DESC
            """,
            nativeQuery = true)
    List<Song> search(@Param("kw") String keyword, Pageable pageable);
}
