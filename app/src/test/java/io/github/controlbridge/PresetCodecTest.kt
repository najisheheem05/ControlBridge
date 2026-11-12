package io.github.controlbridge

import io.github.controlbridge.models.GamepadKey
import io.github.controlbridge.models.GestureType
import io.github.controlbridge.models.Macro
import io.github.controlbridge.models.MacroStep
import io.github.controlbridge.models.Mapping
import io.github.controlbridge.models.MappingAction
import io.github.controlbridge.models.Mode
import io.github.controlbridge.models.Profile
import io.github.controlbridge.profile.PresetCodec
import io.github.controlbridge.profile.ProfileStorage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetCodecTest {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Test
    fun fileRoundTripPreservesProfile() {
        val original = ProfileStorage.createDefaultEFootball("eFootball Share")

        val decoded = PresetCodec.decode(PresetCodec.encode(original)).getOrThrow()

        assertEquals(original.name, decoded.name)
        assertEquals(original.modes.size, decoded.modes.size)
        assertEquals(original.layout.elements.size, decoded.layout.elements.size)
        assertEquals(original.activeModeId, decoded.activeModeId)

        val originalShot = original.modes.first { it.name == "Standard style" }
            .mappings.first { it.controlId == "btn_x" && it.gesture == GestureType.SWIPE_RIGHT }
        val decodedShot = decoded.modes.first { it.name == "Standard style" }
            .mappings.first { it.controlId == "btn_x" && it.gesture == GestureType.SWIPE_RIGHT }

        assertEquals(originalShot.action, decodedShot.action)
        assertEquals(
            original.modes.first { it.name == "Standard style" }.guideText,
            decoded.modes.first { it.name == "Standard style" }.guideText
        )
    }

    @Test
    fun linkRoundTripPreservesMacros() {
        val original = profileWithMacro()

        val decoded = PresetCodec.decodeLink(PresetCodec.encodeLink(original)).getOrThrow()

        assertEquals(original.name, decoded.name)
        assertEquals(1, decoded.modes.size)
        assertEquals(1, decoded.modes.first().macros.size)
        assertEquals("Combo", decoded.modes.first().macros.first().name)
        assertEquals(
            MappingAction.TriggerMacro(decoded.modes.first().macros.first().id),
            decoded.modes.first().mappings.first().action
        )
    }

    @Test
    fun importAssignsNewProfileId() {
        val original = ProfileStorage.createDefaultProfile("Stable")

        val decoded = PresetCodec.decode(PresetCodec.encode(original)).getOrThrow()

        assertNotEquals(original.id, decoded.id)
        assertEquals(original.modes.first().id, decoded.modes.first().id)
    }

    @Test
    fun rejectsInvalidJson() {
        assertTrue(PresetCodec.decode("not a preset").isFailure)
        assertTrue(PresetCodec.decodeLink("controlbridge://preset?d=%%%").isFailure)
    }

    @Test
    fun rejectsWrongFormat() {
        val text = json.encodeToString(
            PresetCodec.PresetPackage(
                format = "some-other-app",
                version = PresetCodec.CURRENT_VERSION,
                profile = ProfileStorage.createDefaultProfile("Wrong format")
            )
        )

        assertTrue(PresetCodec.decode(text).isFailure)
    }

    @Test
    fun rejectsNewerVersion() {
        val text = json.encodeToString(
            PresetCodec.PresetPackage(
                format = PresetCodec.FORMAT,
                version = PresetCodec.CURRENT_VERSION + 1,
                profile = ProfileStorage.createDefaultProfile("Future")
            )
        )

        assertTrue(PresetCodec.decode(text).isFailure)
    }

    @Test
    fun dropsBrokenMacroReference() {
        val base = ProfileStorage.createDefaultProfile("Broken macro")
        val mode = base.modes.first().copy(
            mappings = listOf(
                Mapping(
                    controlId = "btn_a",
                    gesture = GestureType.TAP,
                    action = MappingAction.TriggerMacro("missing-macro")
                )
            )
        )
        val profile = base.copy(modes = listOf(mode), activeModeId = mode.id)

        val decoded = PresetCodec.decode(PresetCodec.encode(profile)).getOrThrow()

        assertTrue(decoded.modes.first().mappings.isEmpty())
    }

    @Test
    fun dropsBrokenModeReference() {
        val base = ProfileStorage.createDefaultProfile("Broken mode")
        val mode = base.modes.first().copy(
            mappings = listOf(
                Mapping(
                    controlId = "btn_a",
                    gesture = GestureType.TAP,
                    action = MappingAction.SwitchMode("missing-mode")
                )
            )
        )
        val profile = base.copy(modes = listOf(mode), activeModeId = mode.id)

        val decoded = PresetCodec.decode(PresetCodec.encode(profile)).getOrThrow()

        assertTrue(decoded.modes.first().mappings.isEmpty())
    }

    @Test
    fun dropsMappingsForMissingControls() {
        val base = ProfileStorage.createDefaultProfile("Missing controls")
        val mode = base.modes.first().copy(
            mappings = listOf(
                Mapping(
                    controlId = "btn_a",
                    gesture = GestureType.TAP,
                    action = MappingAction.ButtonPress(GamepadKey.A)
                ),
                Mapping(
                    controlId = "btn_missing",
                    gesture = GestureType.TAP,
                    action = MappingAction.ButtonPress(GamepadKey.B)
                )
            )
        )
        val profile = base.copy(modes = listOf(mode), activeModeId = mode.id)

        val decoded = PresetCodec.decode(PresetCodec.encode(profile)).getOrThrow()

        assertEquals(listOf("btn_a"), decoded.modes.first().mappings.map { it.controlId })
    }

    @Test
    fun generatesUniqueNames() {
        assertEquals(
            "eFootball (3)",
            ProfileStorage.generateUniqueName(
                "eFootball",
                setOf("eFootball", "eFootball (2)")
            )
        )
    }

    private fun profileWithMacro(): Profile {
        val layout = ProfileStorage.createDefaultLayout("Macro preset")
        val macro = Macro(
            name = "Combo",
            steps = listOf(
                MacroStep.Press(GamepadKey.A),
                MacroStep.Delay(50),
                MacroStep.Release(GamepadKey.A)
            )
        )
        val mode = Mode(
            name = "Default",
            isDefault = true,
            macros = listOf(macro),
            mappings = listOf(
                Mapping(
                    controlId = "btn_a",
                    gesture = GestureType.TAP,
                    action = MappingAction.TriggerMacro(macro.id)
                )
            )
        )
        return Profile(
            name = "Macro preset",
            modes = listOf(mode),
            activeModeId = mode.id,
            layout = layout
        )
    }
}
