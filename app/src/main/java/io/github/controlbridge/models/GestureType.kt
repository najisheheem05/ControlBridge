package io.github.controlbridge.models

import kotlinx.serialization.Serializable

@Serializable
enum class GestureType {
    TAP,
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT
}
