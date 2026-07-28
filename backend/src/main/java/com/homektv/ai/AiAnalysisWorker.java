package com.homektv.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homektv.domain.AiAnalysisTask;
import com.homektv.domain.Song;
import com.homektv.config.AppProperties;
import com.homektv.repo.AiAnalysisTaskRepository;
import com.homektv.repo.SongRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiAnalysisWorker {
    private final AiAnalysisTaskRepository taskRepository;
    private final SongRepository songRepository;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper;
    private final AppProperties properties;
    private final AiAutoApplyPolicy autoApplyPolicy;
    private final AiClassificationApplier classificationApplier;

    public AiAnalysisWorker(AiAnalysisTaskRepository taskRepository, SongRepository songRepository,
                            DeepSeekClient deepSeekClient, ObjectMapper objectMapper, AppProperties properties,
                            AiAutoApplyPolicy autoApplyPolicy, AiClassificationApplier classificationApplier) {
        this.taskRepository = taskRepository;
        this.songRepository = songRepository;
        this.deepSeekClient = deepSeekClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.autoApplyPolicy = autoApplyPolicy;
        this.classificationApplier = classificationApplier;
    }

    @Async
    @Transactional
    public void analyze(Long taskId) {
        AiAnalysisTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) return;
        task.setStatus("processing");
        task.setAttemptCount(task.getAttemptCount() + 1);
        task.setErrorMessage(null);
        taskRepository.save(task);
        try {
            Song song = songRepository.findById(task.getSongId())
                    .orElseThrow(() -> new IllegalStateException("歌曲不存在: " + task.getSongId()));
            AiSongClassification result = deepSeekClient.classify(song);
            task.setResultJson(objectMapper.writeValueAsString(result));
            if (autoApplyPolicy.shouldAutoApply(result)) {
                classificationApplier.apply(task.getSongId(), result);
                task.setStatus("auto_applied");
            } else {
                task.setStatus("review");
            }
        } catch (Exception e) {
            task.setStatus("failed");
            task.setErrorMessage(safeMessage(e));
        }
        taskRepository.save(task);
    }

    private String safeMessage(Exception exception) {
        Throwable value = exception instanceof JsonProcessingException ? exception : exception.getCause();
        String message = value == null ? exception.getMessage() : value.getMessage();
        if (message == null || message.isBlank()) message = exception.getClass().getSimpleName();
        String apiKey = properties.getAi().getApiKey();
        if (!apiKey.isBlank()) message = message.replace(apiKey, "***");
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

}
