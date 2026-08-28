package io.github.controlbridge.engine

import io.github.controlbridge.models.GamepadKey
import io.github.controlbridge.models.GestureType
import io.github.controlbridge.models.MacroStep
import io.github.controlbridge.models.MappingAction
import io.github.controlbridge.models.Mode
import io.github.controlbridge.transport.TransportManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MappingEngine(
    private val macroEngine: MacroEngine,
    private val scope: CoroutineScope
) {
    var onModeSwitchRequest: ((String) -> Unit)? = null

    class ActiveAction(
        val action: MappingAction,
        private val onRelease: () -> Unit
    ) {
        private var released = false

        fun release() {
            if (!released) {
                released = true
                onRelease()
            }
        }
    }

    fun startAction(
        action: MappingAction,
        activeMode: Mode,
        transport: TransportManager?
    ): ActiveAction {
        transport ?: return ActiveAction(action) {}

        return when (action) {
            is MappingAction.ButtonPress -> {
                transport.setButton(action.key.id, true)
                ActiveAction(action) {
                    transport.setButton(action.key.id, false)
                }
            }
            is MappingAction.MultiButton -> {
                action.keys.forEach { transport.setButton(it.id, true) }
                ActiveAction(action) {
                    action.keys.forEach { transport.setButton(it.id, false) }
                }
            }
            is MappingAction.TriggerMacro -> {
                val macro = activeMode.macros.find { it.id == action.macroId }
                val currentlyPressed = mutableSetOf<GamepadKey>()
                var job: Job? = null
                if (macro != null) {
                    job = scope.launch(Dispatchers.Default) {
                        try {
                            for (step in macro.steps) {
                                when (step) {
                                    is MacroStep.Press -> {
                                        transport.setButton(step.key.id, true)
                                        currentlyPressed.add(step.key)
                                    }
                                    is MacroStep.Release -> {
                                        transport.setButton(step.key.id, false)
                                        currentlyPressed.remove(step.key)
                                    }
                                    is MacroStep.Delay -> delay(step.millis)
                                }
                            }
                        } finally {
                            currentlyPressed.forEach { transport.setButton(it.id, false) }
                        }
                    }
                }
                ActiveAction(action) {
                    job?.cancel()
                    currentlyPressed.forEach { transport.setButton(it.id, false) }
                }
            }
            is MappingAction.SwitchMode -> {
                onModeSwitchRequest?.invoke(action.targetModeId)
                ActiveAction(action) {}
            }
        }
    }
}
