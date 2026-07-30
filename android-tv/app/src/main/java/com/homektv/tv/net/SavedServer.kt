package com.homektv.tv.net

import kotlinx.serialization.Serializable

@Serializable
data class SavedServer(
    val hostPort: String,
    val name: String,
)
