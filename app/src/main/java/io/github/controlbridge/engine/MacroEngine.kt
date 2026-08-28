package io.github.controlbridge.engine

import io.github.controlbridge.models.GamepadKey
import io.github.controlbridge.models.Macro
import io.github.controlbridge.models.MacroStep
import io.github.controlbridge.transport.TransportManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MacroEngine(
    private val scope: CoroutineScope
) {
    private var currentJob: Job? = null

    fun execute(macro: Macro?, transport: TransportManager?) {
        macro ?: return
        transport ?: return
        currentJob = scope.launch(Dispatchers.Default) {
            for (step in macro.steps) {
                when (step) {
                    is MacroStep.Press -> transport.setButton(step.key.id, true)
                    is MacroStep.Release -> transport.setButton(step.key.id, false)
                    is MacroStep.Delay -> delay(step.millis)
                }
            }
        }
    }

    fun cancel() {
        currentJob?.cancel()
    }
}
