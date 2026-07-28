package com.homektv.library;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homektv.domain.Setting;
import com.homektv.repo.SettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 系统设置读写（P2.6，详设§8 ADM-03）。settings 表 key→JSONB value。
 */
@Service
public class SettingService {

    public static final Map<String, Object> TRANSCODE_DEFAULTS = Map.of(
            "direct_copy_containers", List.of("mp4", "m4v", "mkv"),
            "direct_copy_video_codecs", List.of("h264", "hevc"),
            "direct_copy_audio_codecs", List.of("aac", "mp3"),
            "transcode_audio_only", false,
            "transcode_output_container", "mkv",
            "transcode_video_codec", "h264",
            "transcode_audio_codec", "aac",
            "transcode_hardware_acceleration", false,
            "transcode_hardware_auto_configured", false
    );
    private static final Set<String> TRANSCODE_KEYS = TRANSCODE_DEFAULTS.keySet();

    private final SettingRepository repo;
    private final ObjectMapper mapper;

    public SettingService(SettingRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    /** 读取全部设置为 map（value 反序列化为对象） */
    @Transactional(readOnly = true)
    public Map<String, Object> getAll() {
        Map<String, Object> out = new HashMap<>(TRANSCODE_DEFAULTS);
        out.put("tv_video_scale_mode", "zoom");
        for (Setting s : repo.findAll()) {
            out.put(s.getKey(), parse(s.getValue()));
        }
        return out;
    }

    /** 批量写入设置 */
    @Transactional
    public void putAll(Map<String, Object> settings) {
        settings.forEach((k, v) -> {
            Setting s = repo.findById(k).orElseGet(() -> {
                Setting ns = new Setting();
                ns.setKey(k);
                return ns;
            });
            s.setValue(write(v));
            repo.save(s);
        });
    }

    @Transactional
    public Map<String, Object> resetTranscodeDefaults() {
        repo.deleteAllById(TRANSCODE_KEYS);
        return getAll();
    }

    public TranscodePolicy transcodePolicy() {
        Map<String, Object> settings = getAll();
        return new TranscodePolicy(
                strings(settings.get("direct_copy_containers"), List.of("mp4", "m4v", "mkv")),
                strings(settings.get("direct_copy_video_codecs"), List.of("h264", "hevc")),
                strings(settings.get("direct_copy_audio_codecs"), List.of("aac", "mp3")),
                Boolean.TRUE.equals(settings.get("transcode_audio_only")),
                option(settings, "transcode_output_container", Set.of("mkv", "mp4"), "mkv"),
                option(settings, "transcode_video_codec", Set.of("h264", "hevc"), "h264"),
                option(settings, "transcode_audio_codec", Set.of("aac", "mp3", "opus"), "aac"),
                Boolean.TRUE.equals(settings.get("transcode_hardware_acceleration"))
        );
    }

    public record TranscodePolicy(List<String> directCopyContainers, List<String> directCopyVideoCodecs,
                                  List<String> directCopyAudioCodecs, boolean transcodeAudioOnly,
                                  String outputContainer, String videoCodec, String audioCodec,
                                  boolean hardwareAcceleration) {}

    private static List<String> strings(Object value, List<String> fallback) {
        if (!(value instanceof List<?> list)) return fallback;
        List<String> result = list.stream().map(String::valueOf).map(String::toLowerCase).distinct().toList();
        return result.isEmpty() ? fallback : result;
    }

    private static String option(Map<String, Object> settings, String key, Set<String> allowed, String fallback) {
        String value = String.valueOf(settings.getOrDefault(key, fallback)).toLowerCase();
        return allowed.contains(value) ? value : fallback;
    }

    private Object parse(String json) {
        try {
            return mapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }

    private String write(Object v) {
        try {
            return mapper.writeValueAsString(v);
        } catch (Exception e) {
            return "null";
        }
    }
}
