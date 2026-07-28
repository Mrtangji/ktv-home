package com.homektv.tv.net

import android.content.Context
import androidx.core.content.edit

/**
 * NAS 服务端地址持久化（详设§12.1 改造）。
 * 首选 [LanScanner] 局域网自动扫描落库，扫不到时回退手输；一经保存即固定。
 */
class AppConfig(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("ktv_tv", Context.MODE_PRIVATE)

    /** 形如 192.168.1.10:8080 的服务端 host:port（已归一化）。未配置时为 null。 */
    var serverHost: String?
        get() = prefs.getString(KEY_HOST, null)
        set(value) = prefs.edit { putString(KEY_HOST, value) }

    val isConfigured: Boolean get() = !serverHost.isNullOrBlank()

    var microphoneMonitorEnabled: Boolean
        get() = prefs.getBoolean(KEY_MICROPHONE_MONITOR, true)
        set(value) = prefs.edit { putBoolean(KEY_MICROPHONE_MONITOR, value) }

    /** WebSocket 地址：ws://host:port/ws?client_type=tv&client_token=xxx */
    fun wsUrl(clientToken: String): String =
        "ws://${serverHost}/ws?client_type=tv&client_token=$clientToken"

    /** REST/资源基址：http://host:port/api */
    fun apiBase(): String = "http://${serverHost}/api"

    /** H5 点歌地址（用于待机页二维码/明文兜底） */
    fun h5Url(): String = "http://${serverHost}/m"

    /** 稳定的设备 token（首次生成后固定），用于 WS client_token。 */
    val clientToken: String
        get() = prefs.getString(KEY_TOKEN, null) ?: run {
            val t = "tv-" + java.util.UUID.randomUUID().toString().take(8)
            prefs.edit { putString(KEY_TOKEN, t) }
            t
        }

    companion object {
        private const val KEY_HOST = "server_host"
        private const val KEY_TOKEN = "client_token"
        private const val KEY_MICROPHONE_MONITOR = "microphone_monitor_enabled"

        /**
         * 归一化用户输入：去空格、剥离 http(s):// 前缀与尾部斜杠；
         * 未带端口时补默认 8080；手动输入支持任意有效服务端端口。
         */
        fun normalizeHost(raw: String): String? {
            var s = raw.trim()
            if (s.isEmpty()) return null
            s = s.removePrefix("http://").removePrefix("https://")
            s = s.substringBefore("/")        // 去掉路径
            if (s.isEmpty()) return null
            if (!s.contains(":")) s = "$s:8080"
            return s
        }
    }
}
