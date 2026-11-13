package io.github.controlbridge.models

enum class GameTemplate(
    val id: String,
    val displayName: String,
    val description: String
) {
    DEFAULT(
        id = "default",
        displayName = "Standard Controller",
        description = "Standard Xbox 360 controller."
    )
    // Add new game templates here in the future:
    // EA_FC(id = "ea_fc", displayName = "EA Sports FC", description = "Classic FC mobile touch controls and skill moves."),
    // RACING(id = "racing", displayName = "Racing / Driving", description = "Steering with analog triggers for throttle & brake.")
}
