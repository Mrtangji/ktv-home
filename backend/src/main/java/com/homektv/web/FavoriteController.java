package com.homektv.web;

import com.homektv.domain.Favorite;
import com.homektv.queue.UserService;
import com.homektv.repo.FavoriteRepository;
import com.homektv.repo.SongRepository;
import com.homektv.web.dto.SongDto;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {
    private final FavoriteRepository favoriteRepo;
    private final SongRepository songRepo;
    private final UserService userService;

    public FavoriteController(FavoriteRepository favoriteRepo, SongRepository songRepo, UserService userService) {
        this.favoriteRepo = favoriteRepo;
        this.songRepo = songRepo;
        this.userService = userService;
    }

    @GetMapping
    public List<SongDto> list(@RequestParam String clientToken) {
        Long userId = requireUser(clientToken);
        return favoriteRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(Favorite::getSongId)
                .map(songRepo::findById)
                .flatMap(java.util.Optional::stream)
                .map(SongDto::from)
                .toList();
    }

    @GetMapping("/ids")
    public List<Long> ids(@RequestParam String clientToken) {
        Long userId = requireUser(clientToken);
        return favoriteRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(Favorite::getSongId)
                .toList();
    }

    @PostMapping("/{songId}")
    @Transactional
    public Map<String, Object> add(@PathVariable Long songId, @RequestBody Map<String, String> body) {
        Long userId = requireUser(body.get("clientToken"));
        if (!songRepo.existsById(songId)) throw new ApiException("SONG_NOT_FOUND", "歌曲不存在");
        favoriteRepo.insertIfAbsent(userId, songId);
        return Map.of("status", "ok", "favorite", true);
    }

    @DeleteMapping("/{songId}")
    @Transactional
    public Map<String, Object> remove(@PathVariable Long songId, @RequestParam String clientToken) {
        Long userId = requireUser(clientToken);
        favoriteRepo.deleteByUserIdAndSongId(userId, songId);
        return Map.of("status", "ok", "favorite", false);
    }

    private Long requireUser(String clientToken) {
        if (clientToken == null || clientToken.isBlank()) {
            throw new ApiException("INVALID_ARGUMENT", "缺少 clientToken");
        }
        return userService.resolveUserId(clientToken);
    }
}
