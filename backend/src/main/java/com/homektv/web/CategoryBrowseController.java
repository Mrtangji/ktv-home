package com.homektv.web;

import com.homektv.library.CategoryBrowseService;
import com.homektv.web.dto.SongDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/browse")
public class CategoryBrowseController {
    private final CategoryBrowseService service;

    public CategoryBrowseController(CategoryBrowseService service) {
        this.service = service;
    }

    @GetMapping("/artists")
    public List<Map<String, Object>> artists() { return service.artists(); }

    @GetMapping("/languages")
    public List<Map<String, Object>> languages() { return service.languages(); }

    @GetMapping("/tags")
    public List<Map<String, Object>> tags() { return service.tags(); }

    @GetMapping("/songs")
    public List<SongDto> songs(@RequestParam(required = false) String artist,
                               @RequestParam(required = false) String language,
                               @RequestParam(required = false) String tag,
                               @RequestParam(required = false) String vocalForm,
                               @RequestParam(defaultValue = "hot") String sort,
                               @RequestParam(defaultValue = "100") int limit) {
        return service.songs(artist, language, tag, vocalForm, sort, limit);
    }
}
