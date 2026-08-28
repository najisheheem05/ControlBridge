package io.github.controlbridge.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Macro(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val steps: List<MacroStep>
)

@Serializable
sealed class MacroStep {
    @Serializable
    @SerialName("press")
    data class Press(val key: GamepadKey) : MacroStep()

    @Serializable
    @SerialName("release")
    data class Release(val key: GamepadKey) : MacroStep()

    @Serializable
    @SerialName("delay")
    data class Delay(val millis: Long) : MacroStep()
}
