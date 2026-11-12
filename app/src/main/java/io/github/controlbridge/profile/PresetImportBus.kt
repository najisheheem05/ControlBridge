package io.github.controlbridge.profile

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface PresetImportEvent {
    data class Payload(val text: String, val fromLink: Boolean) : PresetImportEvent
    data class Error(val message: String) : PresetImportEvent
}

object PresetImportBus {
    private val _pending = MutableStateFlow<PresetImportEvent?>(null)
    val pending: StateFlow<PresetImportEvent?> = _pending

    fun submitFile(text: String) {
        _pending.value = PresetImportEvent.Payload(text, fromLink = false)
    }

    fun submitLink(link: String) {
        _pending.value = PresetImportEvent.Payload(link, fromLink = true)
    }

    fun submitError(message: String) {
        _pending.value = PresetImportEvent.Error(message)
    }

    fun consume() {
        _pending.value = null
    }
}
