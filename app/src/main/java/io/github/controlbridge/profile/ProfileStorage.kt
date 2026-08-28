package io.github.controlbridge.profile

import android.content.Context
import io.github.controlbridge.models.AnalogStickElement
import io.github.controlbridge.models.ButtonElement
import io.github.controlbridge.models.ControllerElement
import io.github.controlbridge.models.ControllerLayout
import io.github.controlbridge.models.GamepadKey
import io.github.controlbridge.models.GameTemplate
import io.github.controlbridge.models.GestureType
import io.github.controlbridge.models.Mapping
import io.github.controlbridge.models.MappingAction
import io.github.controlbridge.models.Mode
import io.github.controlbridge.models.Profile
import kotlinx.serialization.json.Json
import java.io.File

object ProfileStorage {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private const val FILE_NAME = "profiles.json"

    fun load(context: Context): MutableList<Profile> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return mutableListOf()
        return try {
            json.decodeFromString<MutableList<Profile>>(file.readText())
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun load(context: Context, id: String): Profile? {
        return load(context).firstOrNull { it.id == id }
    }

    fun loadByName(context: Context, name: String): Profile? {
        return load(context).firstOrNull { it.name == name }
    }

    fun save(context: Context, profiles: List<Profile>) {
        val file = File(context.filesDir, FILE_NAME)
        file.writeText(json.encodeToString(profiles))
    }

    fun createDefaultLayout(name: String): ControllerLayout {
        return ControllerLayout(
            name = name,
            elements = listOf(
                ButtonElement(id = "btn_a", x = 0.78f, y = 0.67f, size = 0.09f, opacity = 0.85f, key = GamepadKey.A),
                ButtonElement(id = "btn_b", x = 0.87f, y = 0.55f, size = 0.09f, opacity = 0.85f, key = GamepadKey.B),
                ButtonElement(id = "btn_x", x = 0.69f, y = 0.55f, size = 0.09f, opacity = 0.85f, key = GamepadKey.X),
                ButtonElement(id = "btn_y", x = 0.78f, y = 0.45f, size = 0.09f, opacity = 0.85f, key = GamepadKey.Y),
                AnalogStickElement(id = "dpad", x = 0.22f, y = 0.55f, size = 0.18f, opacity = 0.8f),
                ButtonElement(id = "btn_lt", x = 0.10f, y = 0.15f, size = 0.12f, opacity = 0.7f, key = GamepadKey.LT),
                ButtonElement(id = "btn_rt", x = 0.90f, y = 0.15f, size = 0.12f, opacity = 0.7f, key = GamepadKey.RT),
                ButtonElement(id = "btn_lb", x = 0.25f, y = 0.18f, size = 0.12f, opacity = 0.7f, key = GamepadKey.LB),
                ButtonElement(id = "btn_rb", x = 0.75f, y = 0.18f, size = 0.12f, opacity = 0.7f, key = GamepadKey.RB),
                ButtonElement(id = "btn_l3", x = 0.42f, y = 0.25f, size = 0.07f, opacity = 0.6f, key = GamepadKey.L3),
                ButtonElement(id = "btn_r3", x = 0.58f, y = 0.25f, size = 0.07f, opacity = 0.6f, key = GamepadKey.R3),
                ButtonElement(id = "btn_select", x = 0.42f, y = 0.45f, size = 0.07f, opacity = 0.6f, key = GamepadKey.SELECT),
                ButtonElement(id = "btn_start", x = 0.58f, y = 0.45f, size = 0.07f, opacity = 0.6f, key = GamepadKey.START)
            )
        )
    }

    // ──────────────────────────────────────────────
    // eFootball Layout — edit positions, sizes, and enabled/disabled here
    // ──────────────────────────────────────────────
    fun createEFootballLayout(name: String): ControllerLayout {
        return ControllerLayout(
            name = name,
            elements = listOf(
                // Face buttons (A / B / X / Y)
                ButtonElement(id = "btn_a", x = 0.78f, y = 0.67f, size = 0.09f, opacity = 0.85f, key = GamepadKey.A),
                ButtonElement(id = "btn_b", x = 0.87f, y = 0.55f, size = 0.09f, opacity = 0.85f, key = GamepadKey.B),
                ButtonElement(id = "btn_x", x = 0.69f, y = 0.55f, size = 0.09f, opacity = 0.85f, key = GamepadKey.X),
                ButtonElement(id = "btn_y", x = 0.78f, y = 0.45f, size = 0.09f, opacity = 0.85f, key = GamepadKey.Y),
                // Analog stick
                AnalogStickElement(id = "dpad", x = 0.22f, y = 0.55f, size = 0.18f, opacity = 0.8f),
                // Triggers & Bumpers
                ButtonElement(id = "btn_lt", x = 0.10f, y = 0.15f, size = 0.12f, opacity = 0.7f, key = GamepadKey.LT),
                ButtonElement(id = "btn_rt", x = 0.90f, y = 0.15f, size = 0.12f, opacity = 0.7f, key = GamepadKey.RT),
                ButtonElement(id = "btn_lb", x = 0.25f, y = 0.18f, size = 0.12f, opacity = 0.7f, key = GamepadKey.LB),
                ButtonElement(id = "btn_rb", x = 0.75f, y = 0.18f, size = 0.12f, opacity = 0.7f, key = GamepadKey.RB),
                // Stick clicks
                ButtonElement(id = "btn_l3", x = 0.42f, y = 0.25f, size = 0.07f, opacity = 0.6f, key = GamepadKey.L3),
                ButtonElement(id = "btn_r3", x = 0.58f, y = 0.25f, size = 0.07f, opacity = 0.6f, key = GamepadKey.R3),
                // Navigation
                ButtonElement(id = "btn_select", x = 0.42f, y = 0.45f, size = 0.07f, opacity = 0.6f, key = GamepadKey.SELECT),
                ButtonElement(id = "btn_start", x = 0.58f, y = 0.45f, size = 0.07f, opacity = 0.6f, key = GamepadKey.START)
            )
        )
    }

    private fun map(controlId: String, gesture: GestureType, action: MappingAction): Mapping {
        return Mapping(controlId = controlId, gesture = gesture, action = action)
    }

    fun createDefaultEFootball(name: String = "eFootball"): Profile {
        val layout = createEFootballLayout(name)

        // Match Mode Mappings (eFootball Gesture Controls)
        val matchMappings = mutableListOf(
            // Pass (btn_a)
            map("btn_a", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.A)),
            map("btn_a", GestureType.SWIPE_RIGHT, MappingAction.ButtonPress(GamepadKey.Y)),
            map("btn_a", GestureType.SWIPE_UP, MappingAction.ButtonPress(GamepadKey.B)),
            map("btn_a", GestureType.SWIPE_DOWN, MappingAction.MultiButton(listOf(GamepadKey.LB, GamepadKey.Y))),

            // Shoot (btn_x)
            map("btn_x", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.X)),
            map("btn_x", GestureType.SWIPE_RIGHT, MappingAction.MultiButton(listOf(GamepadKey.X, GamepadKey.RT))),
            map("btn_x", GestureType.SWIPE_DOWN, MappingAction.MultiButton(listOf(GamepadKey.RB, GamepadKey.X))),
            map("btn_x", GestureType.SWIPE_LEFT, MappingAction.MultiButton(listOf(GamepadKey.LB, GamepadKey.X))),

            // Cross / Clear (btn_b)
            map("btn_b", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.B)),
            map("btn_b", GestureType.SWIPE_RIGHT, MappingAction.MultiButton(listOf(GamepadKey.B, GamepadKey.RT))),
            map("btn_b", GestureType.SWIPE_DOWN, MappingAction.ButtonPress(GamepadKey.B)),

            // Through Pass (btn_y)
            map("btn_y", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.Y)),
            map("btn_y", GestureType.SWIPE_UP, MappingAction.MultiButton(listOf(GamepadKey.LB, GamepadKey.Y))),

            // Shoulder / Triggers / Navigation
            map("btn_rt", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.RT)),
            map("btn_rb", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.RB)),
            map("btn_lb", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.LB)),
            map("btn_lt", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.LT)),
            map("btn_select", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.SELECT)),
            map("btn_start", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.START)),
            map("btn_l3", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.L3)),
            map("btn_r3", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.R3))
        )

        val defaultMappings = layout.elements
            .filterIsInstance<ButtonElement>()
            .map { btn ->
                map(btn.id, GestureType.TAP, MappingAction.ButtonPress(btn.key))
            }

        val matchMode = Mode(
            name = "Match",
            isDefault = true,
            mappings = matchMappings
        )
        val menuMode = Mode(
            name = "Menu",
            isDefault = false,
            mappings = defaultMappings
        )

        return Profile(
            name = name,
            modes = listOf(matchMode, menuMode),
            activeModeId = matchMode.id,
            layout = layout
        )
    }

    fun createDefaultProfile(name: String): Profile {
        val layout = createDefaultLayout(name)
        val defaultMappings = layout.elements
            .filterIsInstance<ButtonElement>()
            .map { btn ->
                map(btn.id, GestureType.TAP, MappingAction.ButtonPress(btn.key))
            }
        return Profile(
            name = name,
            modes = listOf(
                Mode(name = "Default", isDefault = true, mappings = defaultMappings)
            ),
            layout = layout
        )
    }

    fun createFromTemplate(template: GameTemplate, name: String): Profile {
        return when (template) {
            GameTemplate.DEFAULT -> createDefaultProfile(name)
            GameTemplate.EFOOTBALL -> createDefaultEFootball(name)
        }
    }

    fun Profile.updateElement(
        elementId: String,
        update: (ControllerElement) -> ControllerElement
    ): Profile {
        return copy(
            layout = layout.copy(
                elements = layout.elements.map {
                    if (it.id == elementId) update(it) else it
                }
            ),
            updatedAt = System.currentTimeMillis()
        )
    }

    fun Profile.updateMode(
        modeId: String,
        update: (Mode) -> Mode
    ): Profile {
        return copy(
            modes = modes.map {
                if (it.id == modeId) update(it) else it
            },
            updatedAt = System.currentTimeMillis()
        )
    }

    fun Profile.activeMode(): Mode {
        return modes.find { it.id == activeModeId } ?: modes.first()
    }
}
