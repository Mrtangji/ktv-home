package com.homektv.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 应用配置（app.* 前缀）。
 *
 * Application configuration (app.* prefix).
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** 源素材扫描目录（容器内 /source-music）。Source music scan directory (inside container: /source-music). */
    private String sourceLibraryPath = "/source-music";

    /** TV 实际播放曲库目录（容器内 /music）。TV playback music library directory (inside container: /music). */
    private String ktvLibraryPath = "/music";

    /** 数据缓存目录（封面/歌词）。Data cache directory (covers/lyrics). */
    private String dataPath = "./data";

    /** ffprobe 可执行路径。ffprobe executable path. */
    private String ffprobePath = "ffprobe";

    /** Encrypted application-secret master key file. */
    private String configMasterKeyPath = "/data/secrets/config.key";

    /** AI 曲库分析配置。AI music library analysis configuration. */
    private Ai ai = new Ai();

    /** 局域网服务发现配置。LAN service discovery configuration. */
    private Discovery discovery = new Discovery();

    public String getSourceLibraryPath() { return sourceLibraryPath; }
    public void setSourceLibraryPath(String sourceLibraryPath) { this.sourceLibraryPath = sourceLibraryPath; }
    public String getKtvLibraryPath() { return ktvLibraryPath; }
    public void setKtvLibraryPath(String ktvLibraryPath) { this.ktvLibraryPath = ktvLibraryPath; }
    public String getDataPath() { return dataPath; }
    public void setDataPath(String dataPath) { this.dataPath = dataPath; }
    public String getFfprobePath() { return ffprobePath; }
    public void setFfprobePath(String ffprobePath) { this.ffprobePath = ffprobePath; }
    public String getConfigMasterKeyPath() { return configMasterKeyPath; }
    public void setConfigMasterKeyPath(String configMasterKeyPath) { this.configMasterKeyPath = configMasterKeyPath; }
    public Ai getAi() { return ai; }
    public void setAi(Ai ai) { this.ai = ai; }
    public Discovery getDiscovery() { return discovery; }
    public void setDiscovery(Discovery discovery) { this.discovery = discovery; }

    /**
     * 局域网服务发现配置。
     *
     * LAN service discovery configuration.
     */
    public static class Discovery {
        /** 是否启用服务发现。Whether service discovery is enabled. */
        private boolean enabled = true;
        /** 实例名称。Instance name. */
        private String instanceName = "家庭KTV";
        /** UDP 广播端口。UDP broadcast port. */
        private int udpPort = 18888;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getInstanceName() { return instanceName; }
        public void setInstanceName(String instanceName) { this.instanceName = instanceName; }
        public int getUdpPort() { return udpPort; }
        public void setUdpPort(int udpPort) { this.udpPort = udpPort; }
    }

    /**
     * AI 曲库分析配置。
     *
     * AI music library analysis configuration.
     */
    public static class Ai {
        /** 是否启用 AI 分析。Whether AI analysis is enabled. */
        private boolean enabled = false;
        /** AI API 基础地址。AI API base URL. */
        private String baseUrl = "";
        /** API 密钥。API key. */
        private String apiKey = "";
        /** 使用的模型名称。Model name to use. */
        private String model = "";
        private String bulkModel = "";
        private String reasoningModel = "";
        private String jsonMode = "AUTO";
        private int bulkConcurrency = 2;
        private int reasoningConcurrency = 1;
        /** 自动采纳置信度阈值。Auto-apply confidence threshold. */
        private double autoApplyConfidence = 0.90;
        /** 连接超时秒数。Connect timeout in seconds. */
        private int connectTimeoutSeconds = 10;
        /** 读取超时秒数。Read timeout in seconds. */
        private int readTimeoutSeconds = 60;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getBulkModel() { return bulkModel == null || bulkModel.isBlank() ? model : bulkModel; }
        public void setBulkModel(String bulkModel) { this.bulkModel = bulkModel; }
        public String getReasoningModel() { return reasoningModel; }
        public void setReasoningModel(String reasoningModel) { this.reasoningModel = reasoningModel; }
        public String getJsonMode() { return jsonMode; }
        public void setJsonMode(String jsonMode) { this.jsonMode = jsonMode; }
        public int getBulkConcurrency() { return bulkConcurrency; }
        public void setBulkConcurrency(int bulkConcurrency) { this.bulkConcurrency = bulkConcurrency; }
        public int getReasoningConcurrency() { return reasoningConcurrency; }
        public void setReasoningConcurrency(int reasoningConcurrency) { this.reasoningConcurrency = reasoningConcurrency; }
        public double getAutoApplyConfidence() { return autoApplyConfidence; }
        public void setAutoApplyConfidence(double autoApplyConfidence) { this.autoApplyConfidence = autoApplyConfidence; }
        public int getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
        public void setConnectTimeoutSeconds(int connectTimeoutSeconds) { this.connectTimeoutSeconds = connectTimeoutSeconds; }
        public int getReadTimeoutSeconds() { return readTimeoutSeconds; }
        public void setReadTimeoutSeconds(int readTimeoutSeconds) { this.readTimeoutSeconds = readTimeoutSeconds; }
    }
}
