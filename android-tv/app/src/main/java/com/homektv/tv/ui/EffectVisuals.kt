package com.homektv.tv.ui

enum class EffectVisualKind { CLAP, CHEER, BOO, TOAST }

data class EffectVisualSpec(
    val id: String,
    val kind: EffectVisualKind,
    val title: String,
    val emoji: String,
    val durationMs: Long,
    val particleCount: Int,
    val colors: List<Long>,
)

object EffectVisuals {
    private val specs = listOf(
        EffectVisualSpec("clap", EffectVisualKind.CLAP, "掌声响起来！", "👏", 1_900L, 42,
            listOf(0xffffd54f, 0xffffa726, 0xffffffff)),
        EffectVisualSpec("cheer", EffectVisualKind.CHEER, "太棒了！", "🎉", 2_200L, 90,
            listOf(0xffff4d6d, 0xffffd166, 0xff06d6a0, 0xff4cc9f0, 0xffa78bfa)),
        EffectVisualSpec("boo", EffectVisualKind.BOO, "倒彩时间！", "😜", 1_900L, 36,
            listOf(0xffa78bfa, 0xff34d399, 0xffff7aa2)),
        EffectVisualSpec("toast", EffectVisualKind.TOAST, "干杯！", "🍻", 2_100L, 48,
            listOf(0xffffd166, 0xffffa62b, 0xfffff3b0)),
    )

    val ids: Set<String> = specs.mapTo(linkedSetOf()) { it.id }

    fun find(effectId: String): EffectVisualSpec? = specs.firstOrNull { it.id == effectId.lowercase() }
}
