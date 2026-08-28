package io.github.controlbridge

import androidx.compose.ui.geometry.Offset
import io.github.controlbridge.input.GestureRecognizer
import io.github.controlbridge.models.GestureType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GestureRecognizerTest {
    private val recognizer = GestureRecognizer(
        swipeThresholdPx = 28f
    )

    @Test
    fun testCheckSwipeRealtime() {
        val start = Offset(100f, 100f)
        // Movement below threshold (15px) -> null
        assertNull(recognizer.checkSwipe(start, Offset(115f, 100f)))

        // Movement crossing threshold (50px down) -> SWIPE_DOWN immediately
        assertEquals(GestureType.SWIPE_DOWN, recognizer.checkSwipe(start, Offset(100f, 150f)))

        // Movement crossing threshold (50px up) -> SWIPE_UP immediately
        assertEquals(GestureType.SWIPE_UP, recognizer.checkSwipe(start, Offset(100f, 50f)))

        // Movement crossing threshold (50px right) -> SWIPE_RIGHT immediately
        assertEquals(GestureType.SWIPE_RIGHT, recognizer.checkSwipe(start, Offset(150f, 100f)))

        // Movement crossing threshold (50px left) -> SWIPE_LEFT immediately
        assertEquals(GestureType.SWIPE_LEFT, recognizer.checkSwipe(start, Offset(50f, 100f)))
    }
}
