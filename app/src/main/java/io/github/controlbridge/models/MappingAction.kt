package io.github.controlbridge.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class MappingAction {
    @Serializable
    @SerialName("button")
    data class ButtonPress(val key: GamepadKey) : MappingAction()

    @Serializable
    @SerialName("multi_button")
    data class MultiButton(
        val keys: List<GamepadKey>
    ) : MappingAction()

    @Serializable
    @SerialName("macro")
    data class TriggerMacro(val macroId: String) : MappingAction()

    @Serializable
    @SerialName("mode_switch")
    data class SwitchMode(val targetModeId: String) : MappingAction()
}
