package com.homektv.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homektv.config.AppProperties;
import com.homektv.domain.Song;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class DeepSeekClient {
    private final AppProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    public DeepSeekClient(AppProperties properties, ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClientBuilder = restClientBuilder;
    }

    public AiSongClassification classify(Song song) {
        AppProperties.Ai ai = properties.getAi();
        if (!ai.isEnabled()) throw new IllegalStateException("AI 分析未启用");
        if (ai.getApiKey() == null || ai.getApiKey().isBlank()) throw new IllegalStateException("AI API Key 未配置");

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(ai.getConnectTimeoutSeconds()))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(ai.getReadTimeoutSeconds()));
        RestClient client = restClientBuilder.baseUrl(ai.getBaseUrl()).requestFactory(requestFactory).build();
        Map<String, Object> body = Map.of(
                "model", ai.getModel(),
                "temperature", 0.1,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt()),
                        Map.of("role", "user", "content", userPrompt(song))));

        JsonNode response = client.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ai.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        if (response == null) throw new IllegalStateException("AI 服务返回空响应");
        String content = response.path("choices").path(0).path("message").path("content").asText();
        if (content.isBlank()) throw new IllegalStateException("AI 服务未返回分类结果");
        try {
            return objectMapper.readValue(stripCodeFence(content), AiSongClassification.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("AI 分类结果不是有效 JSON", e);
        }
    }

    private String systemPrompt() {
        return """
                你是家庭KTV曲库整理助手。只返回JSON，不要Markdown。根据歌曲标题、歌手和已有元数据进行分类；不确定的单个字段使用“未知”，不要编造事实。
                JSON字段固定为：language(string)、era(string)、genres(string[])、themes(string[])、ageRange(string)、vocalForm(string)、recommendedPlaylists(string[])、reason(string)、confidence(number)。
                language示例：国语/粤语/闽南语/英语/日语/韩语/其他；era示例：70年代及以前/80年代/90年代/00年代/10年代/20年代/未知；
                ageRange示例：全年龄/成人向/儿童；vocalForm示例：独唱/对唱/合唱/组合/未知。genres、themes和recommendedPlaylists各不超过5项。
                输入中的“当前语种”“现有标签”以及标题内的“-国语-流行”“-粤语-摇滚”等结构化标记视为可信曲库元数据；标题中的(MTV)、(演)、[LIVE]、版本和画面标记不影响歌曲身份。
                confidence取0到1之间的小数，表示这条结果能否直接用于曲库分类，而不是要求每个字段都有百科级确定性：
                1. 标题、歌手明确，且语言或曲风可由可信元数据直接得到时，通常给0.92到0.98；允许年代、主题或演唱形式使用“未知”，不要因此降低整体置信度。
                2. 标题、歌手明确，但只有少量主题需要从歌名合理概括时，通常给0.90到0.94。
                3. 只有存在同名歌曲且无法区分、歌手未知、输入互相冲突，或歌曲身份本身不明确时，才给低于0.90。
                4. 不要为了显得保守而固定返回0.85；应充分利用输入中已有的明确元数据。
                """;
    }

    private String userPrompt(Song song) {
        return "歌曲：" + song.getTitle() + "\n歌手：" + song.getArtist()
                + "\n当前语种：" + song.getLanguage()
                + "\n媒体类型：" + song.getMediaType()
                + "\n现有标签：" + String.join("、", song.getTags());
    }

    private String stripCodeFence(String content) {
        String value = content.trim();
        if (!value.startsWith("```")) return value;
        int firstLine = value.indexOf('\n');
        int endFence = value.lastIndexOf("```");
        return firstLine >= 0 && endFence > firstLine ? value.substring(firstLine + 1, endFence).trim() : value;
    }
}
