package com.project.chat.presentation.ui.chatDetail.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

/**
 * A simple live waveform: evenly spaced vertical bars whose heights come from [amplitudes] (each 0f..1f).
 * The most recent samples sit on the right; older ones scroll off the left. Empty slots render as a
 * minimal baseline tick so the bar always reads as a waveform.
 */
@Composable
fun WaveformView(
    amplitudes: List<Float>,
    barColor: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 48,
) {
    Canvas(modifier = modifier) {
        if (barCount <= 0) return@Canvas
        val slot = size.width / barCount
        val barWidth = slot * 0.5f
        val minBarHeight = barWidth.coerceAtLeast(1f)

        val recent = amplitudes.takeLast(barCount)
        val padded = List(barCount - recent.size) { 0f } + recent

        padded.forEachIndexed { index, amplitude ->
            val barHeight = (amplitude.coerceIn(0f, 1f) * size.height).coerceAtLeast(minBarHeight)
            val left = index * slot + (slot - barWidth) / 2f
            val top = (size.height - barHeight) / 2f
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}
