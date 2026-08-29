package io.github.controlbridge.ui.remap

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.controlbridge.R
import io.github.controlbridge.models.*
import io.github.controlbridge.profile.ProfileStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroListScreen(
    profileId: String,
    modeId: String,
    navigateBack: () -> Unit
) {
    val context = LocalContext.current
    var profile by remember {
        mutableStateOf(ProfileStorage.load(context, profileId)!!)
    }
    val mode = remember(profile, modeId) {
        profile.modes.find { it.id == modeId } ?: profile.modes.first()
    }

    var editingMacro by remember { mutableStateOf<Macro?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    fun saveProfile(updatedProfile: Profile) {
        profile = updatedProfile
        val profiles = ProfileStorage.load(context)
        val idx = profiles.indexOfFirst { it.id == profile.id }
        if (idx != -1) {
            profiles[idx] = updatedProfile
            ProfileStorage.save(context, profiles)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Macros - ${mode.name}") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(painterResource(R.drawable.ic_keyboard_arrow_left), "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingMacro = null
                showEditor = true
            }) {
                Text("+")
            }
        }
    ) { padding ->
        if (mode.macros.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No macros yet. Tap + to create one.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(mode.macros, key = { it.id }) { macro ->
                    MacroCard(
                        macro = macro,
                        onEdit = {
                            editingMacro = macro
                            showEditor = true
                        },
                        onDelete = {
                            val updatedMode = mode.copy(
                                macros = mode.macros.filter { it.id != macro.id }
                            )
                            val updatedProfile = profile.copy(
                                modes = profile.modes.map {
                                    if (it.id == updatedMode.id) updatedMode else it
                                }
                            )
                            saveProfile(updatedProfile)
                        }
                    )
                }
            }
        }
    }

    if (showEditor) {
        MacroEditorSheet(
            macro = editingMacro,
            onSave = { savedMacro ->
                val updatedMacros = if (editingMacro != null) {
                    mode.macros.map { if (it.id == savedMacro.id) savedMacro else it }
                } else {
                    mode.macros + savedMacro
                }
                val updatedMode = mode.copy(macros = updatedMacros)
                val updatedProfile = profile.copy(
                    modes = profile.modes.map {
                        if (it.id == updatedMode.id) updatedMode else it
                    }
                )
                saveProfile(updatedProfile)
                showEditor = false
            },
            onDismiss = { showEditor = false }
        )
    }
}

@Composable
fun MacroCard(
    macro: Macro,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(macro.name, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    macro.steps.joinToString(" → ") { step ->
                        when (step) {
                            is MacroStep.Press -> "Press ${step.key.name}"
                            is MacroStep.Release -> "Release ${step.key.name}"
                            is MacroStep.Delay -> "${step.millis}ms"
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            IconButton(onClick = onEdit) {
                Icon(painterResource(R.drawable.ic_edit), "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(painterResource(io.github.compose_preferences.R.drawable.ic_delete), "Delete")
            }
        }
    }
}
