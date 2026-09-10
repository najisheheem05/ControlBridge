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
        assertEquals(3, profile.modes.size)

        val standardMode = profile.modes.find { it.name == "Standard style" }
        assertNotNull(standardMode)
        assertTrue(standardMode!!.isDefault)

        val pressureMode = profile.modes.find { it.name == "Pressure style" }
        assertNotNull(pressureMode)
        assertFalse(pressureMode!!.isDefault)

        val menuMode = profile.modes.find { it.name == "Menu" }
        assertNotNull(menuMode)
        assertFalse(menuMode!!.isDefault)

        // Verify swipe mappings exist in Standard style mode
        val stunningShotMapping = standardMode.mappings.find {
            it.controlId == "btn_x" && it.gesture == GestureType.SWIPE_RIGHT
        }
        assertNotNull(stunningShotMapping)
        assertTrue(stunningShotMapping!!.action is MappingAction.MultiButton)
        val multiAction = stunningShotMapping.action as MappingAction.MultiButton
        assertTrue(GamepadKey.X in multiAction.keys && GamepadKey.RT in multiAction.keys)

        // Verify Menu mode has only pure TAP mappings
        assertTrue(menuMode.mappings.all { it.gesture == GestureType.TAP })
    }

    @Test
    fun testCreateFromTemplate() {
        val defaultProfile = ProfileStorage.createFromTemplate(GameTemplate.DEFAULT, "MyXbox")
        assertEquals("MyXbox", defaultProfile.name)
        assertEquals(1, defaultProfile.modes.size)

        val efootballProfile = ProfileStorage.createFromTemplate(GameTemplate.EFOOTBALL, "MyEfootball")
        assertEquals("MyEfootball", efootballProfile.name)
        assertEquals(3, efootballProfile.modes.size)
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

    @Test
    fun testDpadButtonsSerialization() {
        val profile = ProfileStorage.createDefaultProfile("DpadTest")
        val dpad = profile.layout.elements.find { it is DpadElement } as? DpadElement
        assertNotNull(dpad)

        val encoded = json.encodeToString(profile)
        val decoded = json.decodeFromString<Profile>(encoded)

        val decodedDpad = decoded.layout.elements.find { it is DpadElement } as? DpadElement
        assertNotNull(decodedDpad)
        assertEquals(dpad!!.size, decodedDpad!!.size, 0.001f)

        val defaultMode = decoded.modes.first()
        val dpadUpMapping = defaultMode.mappings.find { it.controlId == "${dpad.id}_up" }
        assertNotNull(dpadUpMapping)
        assertEquals(MappingAction.ButtonPress(GamepadKey.DPAD_UP), dpadUpMapping!!.action)
    }
}
