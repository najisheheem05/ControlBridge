package io.github.controlbridge.ui.remap

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.controlbridge.R
import io.github.controlbridge.models.*
import io.github.controlbridge.profile.ProfileStorage
import io.github.controlbridge.profile.ProfileStorage.activeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemapScreen(
    profileId: String,
    navigateBack: () -> Unit,
    navigateToMacros: (String, String) -> Unit  // profileId, modeId
) {
    val context = LocalContext.current
    var profile by remember {
        mutableStateOf(ProfileStorage.load(context, profileId)!!)
    }
    var selectedModeId by remember { mutableStateOf(profile.activeModeId) }
    val activeMode = remember(selectedModeId, profile) {
        profile.modes.find { it.id == selectedModeId } ?: profile.modes.first()
    }
    val buttons = remember(profile) {
        profile.layout.elements.filterIsInstance<ButtonElement>()
    }

    var editingControl by remember { mutableStateOf<String?>(null) }
    var editingGesture by remember { mutableStateOf<GestureType?>(null) }
    var showMappingEditor by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Remap Controls") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(painterResource(R.drawable.ic_keyboard_arrow_left), "Back")
                    }
                },
                actions = {
                    // Mode selector dropdown
                    var modeDropdownExpanded by remember { mutableStateOf(false) }
                    TextButton(onClick = { modeDropdownExpanded = true }) {
                        Text("Mode: ${activeMode.name}")
                    }
                    DropdownMenu(
                        expanded = modeDropdownExpanded,
                        onDismissRequest = { modeDropdownExpanded = false }
                    ) {
                        profile.modes.forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        mode.name,
                                        fontWeight = if (mode.id == selectedModeId) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    selectedModeId = mode.id
                                    modeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            buttons.forEach { button ->
                ControlGestureSection(
                    button = button,
                    activeMode = activeMode,
                    onEditGesture = { gesture ->
                        editingControl = button.id
                        editingGesture = gesture
                        showMappingEditor = true
                    }
                )
                Spacer(Modifier.height(16.dp))
            }

            // Analog stick section
            Text(
                "Analog Stick",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                "Maps directly to left analog axis (no gesture remapping)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            // Macro management button
            OutlinedButton(
                onClick = { navigateToMacros(profileId, selectedModeId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Manage Macros")
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // Mapping editor bottom sheet
    if (showMappingEditor && editingControl != null && editingGesture != null) {
        MappingEditorSheet(
            controlId = editingControl!!,
            gesture = editingGesture!!,
            currentMapping = activeMode.mappings.find {
                it.controlId == editingControl && it.gesture == editingGesture
            },
            availableMacros = activeMode.macros,
            modes = profile.modes,
            onSave = { newMapping ->
                val updatedMappings = activeMode.mappings
                    .filter { !(it.controlId == editingControl && it.gesture == editingGesture) }
                    .toMutableList()
                if (newMapping != null) {
                    updatedMappings.add(newMapping)
                }
                val updatedMode = activeMode.copy(mappings = updatedMappings)
                profile = profile.copy(
                    modes = profile.modes.map { if (it.id == updatedMode.id) updatedMode else it },
                    updatedAt = System.currentTimeMillis()
                )
                // Persist
                val profiles = ProfileStorage.load(context)
                val idx = profiles.indexOfFirst { it.id == profile.id }
                if (idx != -1) {
                    profiles[idx] = profile
                    ProfileStorage.save(context, profiles)
                }
                showMappingEditor = false
            },
            onDismiss = { showMappingEditor = false }
        )
    }
}

@Composable
fun ControlGestureSection(
    button: ButtonElement,
    activeMode: Mode,
    onEditGesture: (GestureType) -> Unit
) {
    Text(
        "Button ${button.key.name}",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    GestureType.entries.forEach { gesture ->
        val mapping = activeMode.mappings.find {
            it.controlId == button.id && it.gesture == gesture
        }
        GestureRow(
            gestureName = gesture.displayName(),
            actionText = mapping?.action?.displayText() ?: "(unmapped)",
            isMapped = mapping != null,
            onEdit = { onEditGesture(gesture) }
        )
    }
}

@Composable
fun GestureRow(
    gestureName: String,
    actionText: String,
    isMapped: Boolean,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            gestureName,
            modifier = Modifier.width(100.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            "→",
            modifier = Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            actionText,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isMapped) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onEdit) {
            Text("Edit")
        }
    }
}

fun GestureType.displayName(): String = when (this) {
    GestureType.TAP -> "TAP"
    GestureType.SWIPE_UP -> "SWIPE UP"
    GestureType.SWIPE_DOWN -> "SWIPE DOWN"
    GestureType.SWIPE_LEFT -> "SWIPE LEFT"
    GestureType.SWIPE_RIGHT -> "SWIPE RIGHT"
}

fun MappingAction.displayText(): String = when (this) {
    is MappingAction.ButtonPress -> key.name
    is MappingAction.MultiButton -> keys.joinToString(" + ") { it.name }
    is MappingAction.TriggerMacro -> "Macro: $macroId"
    is MappingAction.SwitchMode -> "Switch Mode"
}
