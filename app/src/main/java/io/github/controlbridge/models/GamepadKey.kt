package io.github.controlbridge.models

import kotlinx.serialization.Serializable

@Serializable
enum class GamepadKey(val mask: Int) {
    A(0x1000),
    B(0x2000),
    X(0x4000),
    Y(0x8000),
    L3(0x0040),
    R3(0x0080),
    LT(7),
    RT(8),
    LB(0x0100),
    RB(0x0200),
    START(0x0010),
    SELECT(0x0020);

    val id: Int get() = mask
}
