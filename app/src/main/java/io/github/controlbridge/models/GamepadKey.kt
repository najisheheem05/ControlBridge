package io.github.controlbridge.models

import kotlinx.serialization.Serializable

@Serializable
enum class GamepadKey(val mask: Int) {
    A(0x1000),
    B(0x2000),
    X(0x4000),
    Y(0x8000),
    DPAD_UP(0x0001),
    DPAD_DOWN(0x0002),
    DPAD_LEFT(0x0004),
    DPAD_RIGHT(0x0008),
    L3(0x0040),
    R3(0x0080),
    LT(0x10000),
    RT(0x20000),
    LB(0x0100),
    RB(0x0200),
    START(0x0010),
    SELECT(0x0020),
    MENU(0x0010),
    VIEW(0x0020);

    val id: Int get() = mask
}
