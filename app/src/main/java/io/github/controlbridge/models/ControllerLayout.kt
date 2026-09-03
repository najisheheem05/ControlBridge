package io.github.controlbridge.models

import android.annotation.SuppressLint
import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ControllerLayout(
    val name: String,
    val elements: List<ControllerElement>
)

@Immutable
@Serializable
sealed class ControllerElement {
    abstract val id: String
    abstract val x: Float
    abstract val y: Float
    abstract val size: Float
    abstract val opacity: Float
    abstract val enabled: Boolean
}

@Immutable
@SuppressLint("UnsafeOptInUsageError")
@Serializable
@SerialName("button")
data class ButtonElement(
    override val id: String,
    override val x: Float,
    override val y: Float,
    override val size: Float,
    override val opacity: Float,
    override val enabled: Boolean = true,
    val key: GamepadKey
) : ControllerElement()

@Immutable
@SuppressLint("UnsafeOptInUsageError")
@Serializable
@SerialName("dpad")
data class AnalogStickElement(
    override val id: String,
    override val x: Float,
    override val y: Float,
    override val size: Float,
    override val opacity: Float,
    override val enabled: Boolean = true
) : ControllerElement()

@Immutable
@SuppressLint("UnsafeOptInUsageError")
@Serializable
@SerialName("dpad_cluster")
data class DpadElement(
    override val id: String = "dpad_buttons",
    override val x: Float,
    override val y: Float,
    override val size: Float,
    override val opacity: Float,
    override val enabled: Boolean = true
) : ControllerElement() {
    fun subButtons(): List<ButtonElement> {
        val dx = size * 0.32f
        val dy = size * 0.32f
        val subSize = size * 0.38f
        return listOf(
            ButtonElement(id = "${id}_up", x = x, y = y - dy, size = subSize, opacity = opacity, enabled = enabled, key = GamepadKey.DPAD_UP),
            ButtonElement(id = "${id}_down", x = x, y = y + dy, size = subSize, opacity = opacity, enabled = enabled, key = GamepadKey.DPAD_DOWN),
            ButtonElement(id = "${id}_left", x = x - dx, y = y, size = subSize, opacity = opacity, enabled = enabled, key = GamepadKey.DPAD_LEFT),
            ButtonElement(id = "${id}_right", x = x + dx, y = y, size = subSize, opacity = opacity, enabled = enabled, key = GamepadKey.DPAD_RIGHT)
        )
    }
}
