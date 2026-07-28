package com.homektv.ai;

import com.homektv.config.AppProperties;
import org.springframework.stereotype.Component;

@Component
public class AiAutoApplyPolicy {
    private final AppProperties properties;

    public AiAutoApplyPolicy(AppProperties properties) {
        this.properties = properties;
    }

    public boolean shouldAutoApply(AiSongClassification result) {
        return result != null && result.confidence() >= threshold();
    }

    public double threshold() {
        return Math.max(0.0, Math.min(1.0, properties.getAi().getAutoApplyConfidence()));
    }
}
