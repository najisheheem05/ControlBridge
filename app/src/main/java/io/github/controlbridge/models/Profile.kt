package io.github.controlbridge.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Profile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val modes: List<Mode> = listOf(Mode(name = "Default", isDefault = true)),
    val activeModeId: String = modes.first().id,
    val layout: ControllerLayout,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
