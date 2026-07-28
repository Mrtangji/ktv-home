package com.homektv.tv.player

object LyricSweepCalculator {
    data class Anchor(val timeMs: Long, val offset: Float)

    fun offsetAt(positionMs: Long, anchors: List<Anchor>, totalWidth: Float): Float {
        if (totalWidth <= 0f || anchors.isEmpty()) return 0f
        val normalized = anchors
            .sortedBy { it.timeMs }
            .fold(mutableListOf<Anchor>()) { result, anchor ->
                val clamped = anchor.copy(offset = anchor.offset.coerceIn(0f, totalWidth))
                if (result.lastOrNull()?.timeMs == clamped.timeMs) {
                    result[result.lastIndex] = clamped.copy(offset = maxOf(result.last().offset, clamped.offset))
                } else {
                    result += clamped
                }
                result
            }
        if (positionMs <= normalized.first().timeMs) return normalized.first().offset
        if (positionMs >= normalized.last().timeMs) return normalized.last().offset
        val rightIndex = normalized.indexOfFirst { it.timeMs > positionMs }
        val left = normalized[rightIndex - 1]
        val right = normalized[rightIndex]
        val fraction = (positionMs - left.timeMs).toFloat() / (right.timeMs - left.timeMs).toFloat()
        return left.offset + (right.offset - left.offset) * fraction
    }
}
