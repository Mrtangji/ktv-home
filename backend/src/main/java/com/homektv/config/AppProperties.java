package com.homektv.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 应用配置（app.* 前缀）。
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** 源素材扫描目录（容器内 /source-music） */
    private String sourceLibraryPath = "/source-music";

    /** TV 实际播放曲库目录（容器内 /music） */
    private String ktvLibraryPath = "/music";

    /** 数据缓存目录（封面/歌词） */
    private String dataPath = "./data";

    /** ffprobe 可执行路径 */
    private String ffprobePath = "ffprobe";

    /** AI 曲库分析配置 */
    private Ai ai = new Ai();

    /** 局域网服务发现配置 */
    private Discovery discovery = new Discovery();

    public String getSourceLibraryPath() { return sourceLibraryPath; }
    public void setSourceLibraryPath(String sourceLibraryPath) { this.sourceLibraryPath = sourceLibraryPath; }
    public String getKtvLibraryPath() { return ktvLibraryPath; }
    public void setKtvLibraryPath(String ktvLibraryPath) { this.ktvLibraryPath = ktvLibraryPath; }
    public String getDataPath() { return dataPath; }
    public void setDataPath(String dataPath) { this.dataPath = dataPath; }
    public String getFfprobePath() { return ffprobePath; }
    public void setFfprobePath(String ffprobePath) { this.ffprobePath = ffprobePath; }
    public Ai getAi() { return ai; }
    public void setAi(Ai ai) { this.ai = ai; }
    public Discovery getDiscovery() { return discovery; }
    public void setDiscovery(Discovery discovery) { this.discovery = discovery; }

    public static class Discovery {
        private boolean enabled = true;
        private String instanceName = "家庭KTV";
        private int udpPort = 18888;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getInstanceName() { return instanceName; }
        public void setInstanceName(String instanceName) { this.instanceName = instanceName; }
        public int getUdpPort() { return udpPort; }
        public void setUdpPort(int udpPort) { this.udpPort = udpPort; }
    }

    public static class Ai {
        private boolean enabled = false;
        private String baseUrl = "https://api.deepseek.com";
        private String apiKey = "";
        private String model = "deepseek-v4-flash";
        private double autoApplyConfidence = 0.90;
        private int connectTimeoutSeconds = 10;
        private int readTimeoutSeconds = 60;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public double getAutoApplyConfidence() { return autoApplyConfidence; }
        public void setAutoApplyConfidence(double autoApplyConfidence) { this.autoApplyConfidence = autoApplyConfidence; }
        public int getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
        public void setConnectTimeoutSeconds(int connectTimeoutSeconds) { this.connectTimeoutSeconds = connectTimeoutSeconds; }
        public int getReadTimeoutSeconds() { return readTimeoutSeconds; }
        public void setReadTimeoutSeconds(int readTimeoutSeconds) { this.readTimeoutSeconds = readTimeoutSeconds; }
    }
}
