package io.github.controlbridge.profile

import io.github.controlbridge.models.AnalogStickElement
import io.github.controlbridge.models.ButtonElement
import io.github.controlbridge.models.ControllerElement
import io.github.controlbridge.models.DpadElement
import io.github.controlbridge.models.MappingAction
import io.github.controlbridge.models.Profile
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.URLDecoder
import java.util.Base64
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object PresetCodec {
    const val FORMAT = "controlbridge-preset"
    const val CURRENT_VERSION = 1
    const val LINK_SCHEME = "controlbridge"
    const val LINK_HOST = "preset"
    const val MAX_PAYLOAD_BYTES = 1_000_000
    const val MAX_LINK_LENGTH = 100_000

    private const val MAX_NAME_LENGTH = 64

    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Serializable
    data class PresetPackage(
        val format: String,
        val version: Int,
        val exportedAt: Long = System.currentTimeMillis(),
        val profile: Profile
    )

    fun encode(profile: Profile): String {
        val text = json.encodeToString(
            PresetPackage(
                format = FORMAT,
                version = CURRENT_VERSION,
                profile = profile
            )
        )
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_PAYLOAD_BYTES) {
            "This preset is too large to export (max 1 MB)."
        }
        return text
    }

    fun decode(text: String): Result<Profile> = runCatching {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_PAYLOAD_BYTES) {
            "Preset is too large (max 1 MB)."
        }
        val preset = json.decodeFromString<PresetPackage>(text)
        validate(preset)
    }

    fun encodeLink(profile: Profile): String {
        val compressed = gzip(encode(profile).toByteArray(Charsets.UTF_8))
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(compressed)
        val link = "$LINK_SCHEME://$LINK_HOST?d=$payload"
        require(link.length <= MAX_LINK_LENGTH) {
            "This preset is too large for a link. Share the preset file instead."
        }
        return link
    }

    fun decodeLink(link: String): Result<Profile> = runCatching {
        val uri = URI(link.trim())
        require(uri.scheme == LINK_SCHEME && uri.host == LINK_HOST) {
            "This is not a ControlBridge preset link."
        }
        val payload = queryParameter(uri.rawQuery, "d")
            ?: throw IllegalArgumentException("The preset link has no data.")
        val compressed = Base64.getUrlDecoder().decode(payload)
        require(compressed.size <= MAX_PAYLOAD_BYTES) { "Preset link is too large." }
        decode(gunzip(compressed).toString(Charsets.UTF_8)).getOrThrow()
    }

    private fun queryParameter(rawQuery: String?, name: String): String? {
        val query = rawQuery ?: return null
        return query.split('&')
            .firstOrNull { it.substringBefore('=') == name }
            ?.substringAfter('=', "")
            ?.takeIf { it.isNotEmpty() }
            ?.let { URLDecoder.decode(it, Charsets.UTF_8.name()) }
    }

    private fun validate(preset: PresetPackage): Profile {
        require(preset.format == FORMAT) { "This file is not a ControlBridge preset." }
        require(preset.version in 1..CURRENT_VERSION) {
            "This preset was made with a newer version of ControlBridge."
        }

        val profile = preset.profile
        require(profile.name.isNotBlank()) { "The preset has no name." }
        require(profile.modes.isNotEmpty()) { "The preset has no modes." }

        val modeIds = profile.modes.map { it.id }.toSet()
        val controlIds = profile.layout.elements.flatMap { element ->
            when (element) {
                is ButtonElement -> listOf(element.id)
                is AnalogStickElement -> listOf(element.id)
                is DpadElement -> listOf(element.id) + element.subButtons().map { it.id }
            }
        }.toSet()

        val modes = profile.modes.map { mode ->
            val macroIds = mode.macros.map { it.id }.toSet()
            mode.copy(
                mappings = mode.mappings.filter { mapping ->
                    mapping.controlId in controlIds && when (val action = mapping.action) {
                        is MappingAction.TriggerMacro -> action.macroId in macroIds
                        is MappingAction.SwitchMode -> action.targetModeId in modeIds
                        else -> true
                    }
                }
            )
        }
        val activeModeId = profile.activeModeId
            .takeIf { id -> modes.any { it.id == id } }
            ?: modes.first().id

        return profile.copy(
            id = UUID.randomUUID().toString(),
            name = profile.name.trim().take(MAX_NAME_LENGTH),
            modes = modes,
            activeModeId = activeModeId,
            layout = profile.layout.copy(
                elements = profile.layout.elements.map { it.sanitized() }
            )
        )
    }

    private fun ControllerElement.sanitized(): ControllerElement = when (this) {
        is ButtonElement -> copy(
            x = x.coerceIn(0f, 1f),
            y = y.coerceIn(0f, 1f),
            size = size.coerceIn(0.01f, 1f),
            opacity = opacity.coerceIn(0f, 1f)
        )
        is AnalogStickElement -> copy(
            x = x.coerceIn(0f, 1f),
            y = y.coerceIn(0f, 1f),
            size = size.coerceIn(0.01f, 1f),
            opacity = opacity.coerceIn(0f, 1f)
        )
        is DpadElement -> copy(
            x = x.coerceIn(0f, 1f),
            y = y.coerceIn(0f, 1f),
            size = size.coerceIn(0.01f, 1f),
            opacity = opacity.coerceIn(0f, 1f)
        )
    }

    private fun gzip(bytes: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { it.write(bytes) }
        return output.toByteArray()
    }

    private fun gunzip(bytes: ByteArray): ByteArray {
        GZIPInputStream(ByteArrayInputStream(bytes)).use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                require(total <= MAX_PAYLOAD_BYTES) { "Preset link is too large." }
                output.write(buffer, 0, read)
            }
            return output.toByteArray()
        }
    }
}
