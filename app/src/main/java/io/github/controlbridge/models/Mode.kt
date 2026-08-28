package io.github.controlbridge.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Mode(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isDefault: Boolean = false,
    val mappings: List<Mapping> = emptyList(),
    val macros: List<Macro> = emptyList(),
    val guideText: String? = null
)
