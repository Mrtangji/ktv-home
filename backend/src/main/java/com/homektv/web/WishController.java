package com.homektv.web;

import com.homektv.domain.Wish;
import com.homektv.queue.UserService;
import com.homektv.repo.WishRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 心愿单（P3.3，详设§11.1）：H5 无结果提交，后台查看/导出。
 */
@RestController
@RequestMapping("/api")
public class WishController {

    private final WishRepository wishRepo;
    private final UserService userService;

    public WishController(WishRepository wishRepo, UserService userService) {
        this.wishRepo = wishRepo;
        this.userService = userService;
    }

    /** 提交心愿（缺歌反馈） */
    @PostMapping("/wishes")
    public Map<String, Object> add(@RequestBody Map<String, String> body) {
        String keyword = body.get("keyword");
        if (keyword == null || keyword.isBlank()) {
            throw new ApiException("INVALID_ARGUMENT", "缺少关键词");
        }
        Wish w = new Wish();
        w.setKeyword(keyword.trim());
        w.setCreatedBy(userService.resolveUserId(body.get("client_token")));
        wishRepo.save(w);
        return Map.of("status", "ok");
    }

    /** 心愿单列表（后台查看/导出） */
    @GetMapping("/wishes")
    public List<Wish> list() {
        return wishRepo.findAllByOrderByCreatedAtDesc();
    }
}
