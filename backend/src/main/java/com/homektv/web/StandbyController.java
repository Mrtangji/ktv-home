package com.homektv.web;

import com.homektv.config.AppProperties;
import com.homektv.library.SettingService;
import com.homektv.library.StandbyContentService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StandbyController {
    private final StandbyContentService service;
    private final SettingService settingService;
    private final Path dataRoot;

    public StandbyController(StandbyContentService service, SettingService settingService, AppProperties properties) {
        this.service = service;
        this.settingService = settingService;
        this.dataRoot = Path.of(properties.getDataPath());
    }

    @GetMapping("/standby/content")
    public Map<String, Object> content() { return service.content(); }

    @PostMapping(value = "/admin/standby/logo", consumes = "multipart/form-data")
    public Map<String, Object> uploadLogo(@RequestPart("file") MultipartFile file) {
        service.uploadLogo(file);
        return service.content();
    }

    @GetMapping("/standby/logo")
    public ResponseEntity<Resource> logo() {
        Object value = settingService.getAll().get("standby_logo_path");
        if (value == null) return ResponseEntity.notFound().build();
        Path file = dataRoot.resolve(value.toString()).normalize();
        if (!file.startsWith(dataRoot.normalize()) || !Files.isReadable(file)) return ResponseEntity.notFound().build();
        String name = file.getFileName().toString().toLowerCase();
        MediaType type = name.endsWith(".png") ? MediaType.IMAGE_PNG : name.endsWith(".webp") ? MediaType.parseMediaType("image/webp") : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).body(new FileSystemResource(file));
    }
}
