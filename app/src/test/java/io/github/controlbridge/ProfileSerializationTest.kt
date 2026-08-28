package io.github.controlbridge

import io.github.controlbridge.models.*
import io.github.controlbridge.profile.ProfileStorage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ProfileSerializationTest {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Test
    fun testDefaultEFootballProfile() {
        val profile = ProfileStorage.createDefaultEFootball()
        assertEquals("eFootball", profile.name)
        assertTrue(profile.modes.isNotEmpty())

        val defaultMode = profile.modes.first()
        assertEquals("Default", defaultMode.name)
        assertTrue(defaultMode.isDefault)

        // Verify tap mappings exist for buttons
        val buttonMappings = defaultMode.mappings.filter { it.gesture == GestureType.TAP }
        assertTrue(buttonMappings.isNotEmpty())

        val btnA = buttonMappings.find { it.controlId == "btn_a" }
        assertNotNull(btnA)
        assertTrue(btnA!!.action is MappingAction.ButtonPress)
        assertEquals(GamepadKey.A, (btnA.action as MappingAction.ButtonPress).key)
    }

    @Test
    fun testMultiButtonMappingSerialization() {
        val multiAction = MappingAction.MultiButton(
            keys = listOf(GamepadKey.A, GamepadKey.Y)
        )
        val mapping = Mapping(
            controlId = "btn_a",
            gesture = GestureType.SWIPE_UP,
            action = multiAction
        )

        val profile = Profile(
            name = "TestProfile",
            layout = ProfileStorage.createDefaultLayout("TestProfile"),
            modes = listOf(
                Mode(
                    name = "Default",
                    isDefault = true,
                    mappings = listOf(mapping)
                )
            )
        )

        val encoded = json.encodeToString(profile)
        val decoded = json.decodeFromString<Profile>(encoded)

        assertEquals("TestProfile", decoded.name)
        val decodedMapping = decoded.modes.first().mappings.first()
        assertEquals("btn_a", decodedMapping.controlId)
        assertEquals(GestureType.SWIPE_UP, decodedMapping.gesture)
        assertTrue(decodedMapping.action is MappingAction.MultiButton)

        val action = decodedMapping.action as MappingAction.MultiButton
        assertEquals(listOf(GamepadKey.A, GamepadKey.Y), action.keys)
    }

    @Test
    fun testMacroSerialization() {
        val macro = Macro(
            name = "ThroughPass",
            steps = listOf(
                MacroStep.Press(GamepadKey.Y),
                MacroStep.Delay(50),
                MacroStep.Release(GamepadKey.Y)
            )
        )

        val mode = Mode(
            name = "Attack",
            macros = listOf(macro),
            mappings = listOf(
                Mapping(
                    controlId = "btn_y",
                    gesture = GestureType.SWIPE_UP,
                    action = MappingAction.TriggerMacro(macro.id)
                )
            )
        )

        val encoded = json.encodeToString(mode)
        val decoded = json.decodeFromString<Mode>(encoded)

        assertEquals("Attack", decoded.name)
        assertEquals(1, decoded.macros.size)
        assertEquals("ThroughPass", decoded.macros.first().name)
        assertEquals(3, decoded.macros.first().steps.size)
    }
}
