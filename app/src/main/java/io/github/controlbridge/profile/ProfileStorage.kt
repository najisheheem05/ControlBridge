package io.github.controlbridge.profile

import android.content.Context
import io.github.controlbridge.models.AnalogStickElement
import io.github.controlbridge.models.ButtonElement
import io.github.controlbridge.models.ControllerLayout
import io.github.controlbridge.models.ControllerElement
import io.github.controlbridge.models.GamepadKey
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

    fun createDefaultEFootball(): Profile {
        val layout = createDefaultLayout("eFootball")
        val defaultMappings = layout.elements
            .filterIsInstance<ButtonElement>()
            .map { btn ->
                Mapping(
                    controlId = btn.id,
                    gesture = GestureType.TAP,
                    action = MappingAction.ButtonPress(btn.key)
                )
            }
        return Profile(
            name = "eFootball",
            modes = listOf(
                Mode(name = "Default", isDefault = true, mappings = defaultMappings)
            ),
            layout = layout
        )
    }

    fun createDefaultProfile(name: String): Profile {
        val layout = createDefaultLayout(name)
        val defaultMappings = layout.elements
            .filterIsInstance<ButtonElement>()
            .map { btn ->
                Mapping(
                    controlId = btn.id,
                    gesture = GestureType.TAP,
                    action = MappingAction.ButtonPress(btn.key)
                )
            }
        return Profile(
            name = name,
            modes = listOf(
                Mode(name = "Default", isDefault = true, mappings = defaultMappings)
            ),
            layout = layout
        )
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
