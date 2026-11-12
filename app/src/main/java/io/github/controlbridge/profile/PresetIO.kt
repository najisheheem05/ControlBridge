package io.github.controlbridge.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import io.github.controlbridge.models.Profile
import java.io.ByteArrayOutputStream
import java.io.File

object PresetIO {
    fun readText(context: Context, uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                require(total <= PresetCodec.MAX_PAYLOAD_BYTES) {
                    "Preset file is too large (max 1 MB)."
                }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        } ?: throw IllegalArgumentException("Could not open the preset file.")

        return bytes.toString(Charsets.UTF_8)
    }

    fun sharePreset(context: Context, profile: Profile) {
        val directory = File(context.cacheDir, "presets")
        check(directory.exists() || directory.mkdirs()) { "Could not prepare the preset file." }

        val file = File(directory, "${safeFileName(profile.name)}.cbpreset")
        file.writeText(PresetCodec.encode(profile))

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${profile.name} ControlBridge preset")
            clipData = ClipData.newUri(context.contentResolver, "preset", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share preset file"))
    }

    fun shareLink(context: Context, profile: Profile) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, PresetCodec.encodeLink(profile))
            putExtra(Intent.EXTRA_SUBJECT, "${profile.name} ControlBridge preset link")
        }
        context.startActivity(Intent.createChooser(intent, "Share preset link"))
    }

    fun copyLink(context: Context, profile: Profile) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "ControlBridge preset link",
                PresetCodec.encodeLink(profile)
            )
        )
        Toast.makeText(context, "Preset link copied", Toast.LENGTH_SHORT).show()
    }

    private fun safeFileName(name: String): String {
        return name.replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .trim('_', '.')
            .ifBlank { "preset" }
            .take(48)
    }
}
