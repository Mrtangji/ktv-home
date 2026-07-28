package com.homektv.repo;

import com.homektv.domain.PlayerState;
import com.homektv.domain.Song;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用真实 PostgreSQL（Testcontainers）验证：
 * 1) Flyway 迁移 + JPA ddl-auto=validate 通过（实体映射与表结构一致）；
 * 2) player_state 单行已初始化；
 * 3) Song 实体（含 text[] tags）可正常读写。
 */
@SpringBootTest
@Testcontainers
class PersistenceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("ktv")
                    .withUsername("ktv")
                    .withPassword("ktv");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private PlayerStateRepository playerStateRepository;

    @Test
    void playerStateSingletonInitialized() {
        PlayerState ps = playerStateRepository.getSingleton();
        assertThat(ps.getId()).isEqualTo(PlayerState.SINGLETON_ID);
        assertThat(ps.getState()).isEqualTo("idle");
        assertThat(ps.getVolume()).isEqualTo(60);
        assertThat(ps.getVocalMode()).isEqualTo("accompaniment");
    }

    @Test
    void songRoundTripWithTagsArray() {
        Song s = new Song();
        s.setTitle("晴天");
        s.setArtist("周杰伦");
        s.setTitlePy("qingtian");
        s.setTitleInit("qt");
        s.setArtistPy("zhoujielun");
        s.setArtistInit("zjl");
        s.setMediaType("KTV_VIDEO");
        s.setHasVocalTrack(true);
        s.setLyricType("word");
        s.setTags(new String[]{"华语", "经典"});
        s.setFingerprint("test-fp-001");

        Song saved = songRepository.save(s);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        Song found = songRepository.findByFingerprint("test-fp-001").orElseThrow();
        assertThat(found.getTitle()).isEqualTo("晴天");
        assertThat(found.getTags()).containsExactly("华语", "经典");
        assertThat(found.getLyricType()).isEqualTo("word");
        assertThat(found.isHasVocalTrack()).isTrue();
    }
}
