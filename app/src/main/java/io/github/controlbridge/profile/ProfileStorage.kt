package io.github.controlbridge.profile

import android.content.Context
import io.github.controlbridge.models.AnalogStickElement
import io.github.controlbridge.models.ButtonElement
import io.github.controlbridge.models.ControllerElement
import io.github.controlbridge.models.ControllerLayout
import io.github.controlbridge.models.DpadElement
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
                ButtonElement(id = "btn_a", x = 0.81f, y = 0.85f, size = 0.11f, opacity = 0.85f, key = GamepadKey.A),
                ButtonElement(id = "btn_b", x = 0.90f, y = 0.675f, size = 0.11f, opacity = 0.85f, key = GamepadKey.B),
                ButtonElement(id = "btn_x", x = 0.72f, y = 0.675f, size = 0.11f, opacity = 0.85f, key = GamepadKey.X),
                ButtonElement(id = "btn_y", x = 0.81f, y = 0.50f, size = 0.11f, opacity = 0.85f, key = GamepadKey.Y),
                AnalogStickElement(id = "dpad", x = 0.12f, y = 0.72f, size = 0.15f, opacity = 0.8f),
                // D-Pad single unified element (4 directional buttons in one cluster)
                DpadElement(id = "dpad_buttons", x = 0.28f, y = 0.72f, size = 0.18f, opacity = 0.8f),
                ButtonElement(id = "btn_lt", x = 0.10f, y = 0.15f, size = 0.13f, opacity = 0.7f, key = GamepadKey.LT),
                ButtonElement(id = "btn_rt", x = 0.90f, y = 0.15f, size = 0.13f, opacity = 0.7f, key = GamepadKey.RT),
                ButtonElement(id = "btn_lb", x = 0.25f, y = 0.18f, size = 0.13f, opacity = 0.7f, key = GamepadKey.LB),
                ButtonElement(id = "btn_rb", x = 0.75f, y = 0.18f, size = 0.13f, opacity = 0.7f, key = GamepadKey.RB),
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
                ButtonElement(id = "btn_a", x = 0.77f, y = 0.82f, size = 0.12f, opacity = 0.85f, key = GamepadKey.A),
                ButtonElement(id = "btn_b", x = 0.91f, y = 0.80f, size = 0.135f, opacity = 0.85f, key = GamepadKey.B),
                ButtonElement(id = "btn_x", x = 0.915f, y = 0.50f, size = 0.12f, opacity = 0.85f, key = GamepadKey.X),
                ButtonElement(id = "btn_y", x = 0.79f, y = 0.55f, size = 0.12f, opacity = 0.85f, key = GamepadKey.Y),
                // Analog stick
                AnalogStickElement(id = "dpad", x = 0.12f, y = 0.72f, size = 0.15f, opacity = 0.8f),
                // D-Pad single unified element (4 directional buttons in one cluster)
                DpadElement(id = "dpad_buttons", x = 0.28f, y = 0.72f, size = 0.18f, opacity = 0.8f),
                // Triggers & Bumpers
                ButtonElement(id = "btn_lt", x = 0.10f, y = 0.15f, size = 0.12f, opacity = 0.7f, enabled = false, key = GamepadKey.LT),
                ButtonElement(id = "btn_rt", x = 0.90f, y = 0.15f, size = 0.12f, opacity = 0.7f, enabled = false, key = GamepadKey.RT),
                ButtonElement(id = "btn_lb", x = 0.25f, y = 0.18f, size = 0.12f, opacity = 0.7f, enabled = false, key = GamepadKey.LB),
                ButtonElement(id = "btn_rb", x = 0.75f, y = 0.18f, size = 0.12f, opacity = 0.7f, enabled = false, key = GamepadKey.RB),
                // Stick clicks
                ButtonElement(id = "btn_l3", x = 0.35f, y = 0.15f, size = 0.07f, opacity = 0.6f, enabled = true, key = GamepadKey.L3),
                ButtonElement(id = "btn_r3", x = 0.65f, y = 0.15f, size = 0.07f, opacity = 0.6f, enabled = true, key = GamepadKey.R3),
                // Navigation
                ButtonElement(id = "btn_select", x = 0.55f, y = 0.15f, size = 0.07f, opacity = 0.6f, enabled = true, key = GamepadKey.SELECT),
                ButtonElement(id = "btn_start", x = 0.45f, y = 0.15f, size = 0.07f, opacity = 0.6f, enabled = true, key = GamepadKey.START)
            )
        )
    }

    fun allButtons(layout: ControllerLayout): List<ButtonElement> {
        return layout.elements.flatMap { el ->
            when (el) {
                is ButtonElement -> listOf(el)
                is DpadElement -> el.subButtons()
                is AnalogStickElement -> emptyList()
            }
        }
    }

    private fun map(controlId: String, gesture: GestureType, action: MappingAction): Mapping {
        return Mapping(controlId = controlId, gesture = gesture, action = action)
    }

    fun createDefaultEFootball(name: String = "eFootball"): Profile {
        val layout = createEFootballLayout(name)

        // Match Mode Mappings (eFootball Gesture Controls)
        val standardMappings = mutableListOf(
            // Dash / Press (btn_a)
            map("btn_b", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.RT)),

            // Pass / Switch (btn_b)
            map("btn_a", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.A)),
            map("btn_a", GestureType.SWIPE_LEFT, MappingAction.MultiButton(listOf(GamepadKey.RT, GamepadKey.A))),
            map("btn_a", GestureType.SWIPE_RIGHT, MappingAction.MultiButton(listOf(GamepadKey.RT, GamepadKey.B))),
            map("btn_a", GestureType.SWIPE_UP, MappingAction.ButtonPress(GamepadKey.B)),

            // Shoot / Tackle (btn_x)
            map("btn_x", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.X)),
            map("btn_x", GestureType.SWIPE_RIGHT, MappingAction.MultiButton(listOf(GamepadKey.RT, GamepadKey.X))),
            map("btn_x", GestureType.SWIPE_LEFT, MappingAction.MultiButton(listOf(GamepadKey.RT, GamepadKey.X))),
            map("btn_x", GestureType.SWIPE_DOWN, MappingAction.MultiButton(listOf(GamepadKey.RB, GamepadKey.X))),
            map("btn_x", GestureType.SWIPE_UP, MappingAction.MultiButton(listOf(GamepadKey.LB, GamepadKey.X))),
            
            // Through Pass / Match-up (btn_y)
            map("btn_y", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.Y)),
            map("btn_y", GestureType.SWIPE_UP, MappingAction.MultiButton(listOf(GamepadKey.LB, GamepadKey.Y))),

            // lhs == rhs
            map("btn_rt", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.RT)),
            map("btn_rb", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.RB)),
            map("btn_lb", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.LB)),
            map("btn_lt", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.LT)),
            map("btn_select", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.SELECT)),
            map("btn_start", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.START)),
            map("btn_l3", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.L3)),
            map("btn_r3", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.R3)),
            map("dpad_buttons_up", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.DPAD_UP)),
            map("dpad_buttons_down", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.DPAD_DOWN)),
            map("dpad_buttons_left", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.DPAD_LEFT)),
            map("dpad_buttons_right", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.DPAD_RIGHT))
        )

        val pressureMappings = mutableListOf(
            // Dash (btn_a)
            map("btn_b", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.RT)),

            // Pass / Switch (btn_b)
            map("btn_a", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.A)),
            map("btn_a", GestureType.SWIPE_LEFT, MappingAction.MultiButton(listOf(GamepadKey.RT, GamepadKey.A))),
            map("btn_a", GestureType.SWIPE_RIGHT, MappingAction.MultiButton(listOf(GamepadKey.RT, GamepadKey.B))),
            map("btn_a", GestureType.SWIPE_UP, MappingAction.ButtonPress(GamepadKey.B)),

            // Shoot / Tackle (btn_x)
            map("btn_x", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.X)),
            map("btn_x", GestureType.SWIPE_RIGHT, MappingAction.MultiButton(listOf(GamepadKey.RT, GamepadKey.X))),
            map("btn_x", GestureType.SWIPE_LEFT, MappingAction.MultiButton(listOf(GamepadKey.RT, GamepadKey.X))),
            map("btn_x", GestureType.SWIPE_DOWN, MappingAction.MultiButton(listOf(GamepadKey.RB, GamepadKey.X))),
            map("btn_x", GestureType.SWIPE_UP, MappingAction.MultiButton(listOf(GamepadKey.LB, GamepadKey.X))),
            
            // Through Pass / press / Match-up  (btn_y)
            map("btn_y", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.Y)),
            map("btn_y", GestureType.SWIPE_UP, MappingAction.MultiButton(listOf(GamepadKey.LB, GamepadKey.Y))),
            map("btn_y", GestureType.SWIPE_RIGHT, MappingAction.ButtonPress(GamepadKey.LB)),

            // lhs == rhs
            map("btn_rt", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.RT)),
            map("btn_rb", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.RB)),
            map("btn_lb", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.LB)),
            map("btn_lt", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.LT)),
            map("btn_select", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.SELECT)),
            map("btn_start", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.START)),
            map("btn_l3", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.L3)),
            map("btn_r3", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.R3)),
            map("dpad_buttons_up", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.DPAD_UP)),
            map("dpad_buttons_down", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.DPAD_DOWN)),
            map("dpad_buttons_left", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.DPAD_LEFT)),
            map("dpad_buttons_right", GestureType.TAP, MappingAction.ButtonPress(GamepadKey.DPAD_RIGHT))
        )

        val defaultMappings = allButtons(layout)
            .map { btn ->
                map(btn.id, GestureType.TAP, MappingAction.ButtonPress(btn.key))
            }

        val standardMode = Mode(
            name = "Standard style",
            isDefault = true,
            mappings = standardMappings,
            // Key guide / Legend displayed on controller UI during match
            guideText = """
                A  pass/switch
                B  dash/press
                X  shoot/tackle
                Y  through/match-up
            """.trimIndent()
        )
        
        val pressureMode = Mode(
            name = "Pressure style",
            isDefault = false,
            mappings = pressureMappings,
            // Key guide / Legend displayed on controller UI during match
            guideText = """
                A  pass/switch
                B  dash
                X  shoot/tackle
                Y  through/press ->match-up
            """.trimIndent()
        )

        val menuMode = Mode(
            name = "Menu",
            isDefault = false,
            mappings = defaultMappings,
            // Key guide / Legend for menu (edit as desired):
            guideText = null
        )

        return Profile(
            name = name,
            modes = listOf(menuMode, standardMode, pressureMode),
            activeModeId = standardMode.id,
            layout = layout
        )
    }

    fun createDefaultProfile(name: String): Profile {
        val layout = createDefaultLayout(name)
        val defaultMappings = allButtons(layout)
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
