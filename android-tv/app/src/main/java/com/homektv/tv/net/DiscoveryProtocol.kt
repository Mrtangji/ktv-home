package com.homektv.tv.net

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object DiscoveryProtocol {
    const val SERVICE_TYPE = "_home-ktv._tcp."
    const val REQUEST = "HOME_KTV_DISCOVER_V1"
    const val UDP_PORT = 18_888

    private val json = Json { ignoreUnknownKeys = true }

    fun parseResponse(payload: String, sourceHost: String): String? = runCatching {
        val root = json.parseToJsonElement(payload).jsonObject
        if (root["service"]?.jsonPrimitive?.content != "home-ktv") return null
        if (root["protocolVersion"]?.jsonPrimitive?.content?.toIntOrNull() != 1) return null
        val port = root["port"]?.jsonPrimitive?.content?.toIntOrNull()
            ?.takeIf { it in 1..65_535 } ?: return null
        "$sourceHost:$port"
    }.getOrNull()
}
