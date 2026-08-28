package io.github.controlbridge.input

import androidx.compose.ui.geometry.Offset
import io.github.controlbridge.models.GestureType
import kotlin.math.abs
import kotlin.math.sqrt

class GestureRecognizer(
    val swipeThresholdPx: Float = 28f
) {
    fun checkSwipe(start: Offset, current: Offset): GestureType? {
        val dx = current.x - start.x
        val dy = current.y - start.y
        val dist = sqrt(dx * dx + dy * dy)

        if (dist < swipeThresholdPx) return null

        return if (abs(dx) > abs(dy)) {
            if (dx > 0) GestureType.SWIPE_RIGHT else GestureType.SWIPE_LEFT
        } else {
            if (dy > 0) GestureType.SWIPE_DOWN else GestureType.SWIPE_UP
        }
    }
}
