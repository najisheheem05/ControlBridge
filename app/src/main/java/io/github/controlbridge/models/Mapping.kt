package io.github.controlbridge.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Mapping(
    val id: String = UUID.randomUUID().toString(),
    val controlId: String,
    val gesture: GestureType,
    val action: MappingAction
)
