package io.github.controlbridge.ui.main

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.controlbridge.R
import io.github.controlbridge.models.GameTemplate
import io.github.controlbridge.models.Profile
import io.github.controlbridge.profile.PresetCodec
import io.github.controlbridge.profile.PresetIO
import io.github.controlbridge.profile.PresetImportBus
import io.github.controlbridge.profile.PresetImportEvent
import io.github.controlbridge.profile.ProfileStorage
import io.github.controlbridge.viewmodel.GPEmulationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfilesScreen(navigateTo: (String) -> Unit, viewModel: GPEmulationViewModel) {
    val context = LocalContext.current
    val profiles = remember {
        mutableStateListOf<Profile>().apply {
            val loaded = ProfileStorage.load(context)
            if (loaded.isEmpty()) {
                // Add default standard controller profile on first launch
                val defaultProfile = ProfileStorage.createDefaultProfile("Default")
                add(defaultProfile)
                ProfileStorage.save(context, listOf(defaultProfile))
            } else {
                addAll(loaded)
            }
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var importCandidate by remember { mutableStateOf<Profile?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    val pendingImport by PresetImportBus.pending.collectAsState()

    fun openImportPreview(text: String, fromLink: Boolean) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                if (fromLink) PresetCodec.decodeLink(text) else PresetCodec.decode(text)
            }
            result.onSuccess { importCandidate = it }
                .onFailure { importError = it.message ?: "This preset could not be read." }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching { PresetIO.readText(context, uri) }
                }
                result.onSuccess { openImportPreview(it, fromLink = false) }
                    .onFailure {
                        importError = it.message ?: "Could not read the preset file."
                    }
            }
        }
    }

    LaunchedEffect(pendingImport) {
        val event = pendingImport ?: return@LaunchedEffect
        PresetImportBus.consume()
        when (event) {
            is PresetImportEvent.Payload -> openImportPreview(event.text, event.fromLink)
            is PresetImportEvent.Error -> importError = event.message
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                ProfileStorage.save(context, profiles.toList())
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.displayCutout),
        topBar = {
            val isActive by viewModel.isReceiverActive.collectAsState()
            var expanded by remember { mutableStateOf(true) }

            LaunchedEffect(isActive, expanded) {
                if (expanded) {
                    delay(3000)
                    expanded = false
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.TopStart
            ) {
                ReceiverStatusChip(
                    isActive = isActive,
                    expanded = expanded,
                    onExpandRequest = { expanded = true }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(painter = painterResource(R.drawable.ic_add), contentDescription = "Create Profile")
            }
        }
    ) { padding ->
        if (profiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No profiles yet")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(profiles, key = { it.id }) { profile ->
                    ProfileCard(
                        profile = profile,
                        onClick = { navigateTo("emulation/${it.id}/false") },
                        onEdit = { navigateTo("emulation/${it.id}/true") },
                        onRemap = { navigateTo("remap/${it.id}") },
                        onShare = { selected ->
                            runCatching { PresetIO.sharePreset(context, selected) }
                                .onFailure { error ->
                                    Toast.makeText(
                                        context,
                                        error.message ?: "Could not share the preset.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        },
                        onShareLink = { selected ->
                            runCatching { PresetIO.shareLink(context, selected) }
                                .onFailure { error ->
                                    Toast.makeText(
                                        context,
                                        error.message ?: "Could not share the preset link.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        },
                        onCopyLink = { selected ->
                            runCatching { PresetIO.copyLink(context, selected) }
                                .onFailure { error ->
                                    Toast.makeText(
                                        context,
                                        error.message ?: "Could not copy the preset link.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        },
                        onDelete = {
                            profiles.remove(it)
                            ProfileStorage.save(context, profiles.toList())
                        }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateProfileDialog(
            existingNames = profiles.map { it.name }.toSet(),
            onDismiss = { showCreateDialog = false },
            onCreate = { template, name ->
                val newProfile = ProfileStorage.createFromTemplate(template, name)
                profiles.add(newProfile)
                ProfileStorage.save(context, profiles.toList())
                showCreateDialog = false
            },
            onImportFile = {
                showCreateDialog = false
                importFileLauncher.launch(
                    arrayOf(
                        "application/vnd.controlbridge.preset+json",
                        "application/json",
                        "application/octet-stream",
                        "*/*"
                    )
                )
            },
            onImportLink = {
                showCreateDialog = false
                showLinkDialog = true
            }
        )
    }

    importCandidate?.let { candidate ->
        ImportPresetDialog(
            profile = candidate,
            existingNames = profiles.map { it.name }.toSet(),
            onDismiss = { importCandidate = null },
            onImport = { name ->
                profiles.add(candidate.copy(name = name))
                ProfileStorage.save(context, profiles.toList())
                importCandidate = null
                Toast.makeText(context, "Preset \"$name\" added", Toast.LENGTH_SHORT).show()
            }
        )
    }

    importError?.let { message ->
        AlertDialog(
            onDismissRequest = { importError = null },
            title = { Text("Import failed", fontWeight = FontWeight.Bold) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { importError = null }) {
                    Text("OK")
                }
            }
        )
    }

    if (showLinkDialog) {
        ImportLinkDialog(
            onDismiss = { showLinkDialog = false },
            onImport = { link ->
                showLinkDialog = false
                openImportPreview(link, fromLink = true)
            }
        )
    }
}

@Composable
private fun ImportLinkDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var link by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Paste Preset Link", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Paste a controlbridge://preset link that was shared with you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("Preset link") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(link.trim()) },
                enabled = link.isNotBlank()
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ImportPresetDialog(
    profile: Profile,
    existingNames: Set<String>,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var presetName by remember(profile) {
        mutableStateOf(ProfileStorage.generateUniqueName(profile.name, existingNames))
    }
    var isNameError by remember { mutableStateOf(false) }
    val mappingCount = profile.modes.sumOf { it.mappings.size }
    val macroCount = profile.modes.sumOf { it.macros.size }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Import Preset", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Only import presets from people you trust.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    "${profile.modes.size} mode${if (profile.modes.size != 1) "s" else ""}, " +
                        "$mappingCount mapping${if (mappingCount != 1) "s" else ""}, " +
                        "$macroCount macro${if (macroCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = presetName,
                    onValueChange = {
                        presetName = it
                        isNameError = it.isBlank() || it.trim() in existingNames
                    },
                    label = { Text("Preset Name") },
                    singleLine = true,
                    isError = isNameError,
                    supportingText = {
                        if (isNameError) {
                            Text(
                                if (presetName.isBlank()) "Name cannot be empty" else "Name already exists",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = presetName.trim()
                    if (trimmed.isNotBlank() && trimmed !in existingNames) {
                        onImport(trimmed)
                    } else {
                        isNameError = true
                    }
                },
                enabled = presetName.trim().isNotBlank() && presetName.trim() !in existingNames
            ) {
                Text("Add Preset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateProfileDialog(
    existingNames: Set<String>,
    onDismiss: () -> Unit,
    onCreate: (GameTemplate, String) -> Unit,
    onImportFile: () -> Unit,
    onImportLink: () -> Unit
) {
    var selectedTemplate by remember { mutableStateOf(GameTemplate.DEFAULT) }
    var profileName by remember {
        mutableStateOf(ProfileStorage.generateUniqueName(selectedTemplate.displayName, existingNames))
    }
    var isNameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("New Controller Profile", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Select Game Preset / Template",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                GameTemplate.entries.forEach { template ->
                    val isSelected = selectedTemplate == template
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedTemplate = template
                                profileName = ProfileStorage.generateUniqueName(template.displayName, existingNames)
                                isNameError = false
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedTemplate = template
                                    profileName = ProfileStorage.generateUniqueName(template.displayName, existingNames)
                                    isNameError = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    template.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    template.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                OutlinedTextField(
                    value = profileName,
                    onValueChange = {
                        profileName = it
                        isNameError = it.isBlank() || it.trim() in existingNames
                    },
                    label = { Text("Profile Name") },
                    singleLine = true,
                    isError = isNameError,
                    supportingText = {
                        if (isNameError) {
                            Text(
                                if (profileName.isBlank()) "Name cannot be empty" else "Name already exists",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                Text(
                    "Or import a shared preset",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Import a .cbpreset file or a controlbridge:// link shared by another player.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onImportFile,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_file_download),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("From file")
                    }
                    OutlinedButton(
                        onClick = onImportLink,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_link),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("From link")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = profileName.trim()
                    if (trimmed.isNotBlank() && trimmed !in existingNames) {
                        onCreate(selectedTemplate, trimmed)
                    } else {
                        isNameError = true
                    }
                },
                enabled = profileName.trim().isNotBlank() && profileName.trim() !in existingNames
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileCard(
    profile: Profile,
    onClick: (Profile) -> Unit,
    onEdit: (Profile) -> Unit,
    onRemap: (Profile) -> Unit,
    onShare: (Profile) -> Unit,
    onShareLink: (Profile) -> Unit,
    onCopyLink: (Profile) -> Unit,
    onDelete: (Profile) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f)
                .combinedClickable(
                    onClick = { onClick(profile) },
                    onLongClick = { menuExpanded = true }
                )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(profile.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${profile.modes.size} mode${if (profile.modes.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Type: XInput, Method: Wifi",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    leadingIcon = { Icon(painter = painterResource(R.drawable.ic_edit), contentDescription = "Edit Layout") },
                    text = { Text("Edit Layout") },
                    onClick = {
                        menuExpanded = false
                        onEdit(profile)
                    }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(painter = painterResource(R.drawable.ic_tune), contentDescription = "Remap") },
                    text = { Text("Remap Controls") },
                    onClick = {
                        menuExpanded = false
                        onRemap(profile)
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    leadingIcon = { Icon(painter = painterResource(R.drawable.ic_share), contentDescription = "Share preset") },
                    text = { Text("Share preset file") },
                    onClick = {
                        menuExpanded = false
                        onShare(profile)
                    }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(painter = painterResource(R.drawable.ic_link), contentDescription = "Share preset link") },
                    text = { Text("Share preset link") },
                    onClick = {
                        menuExpanded = false
                        onShareLink(profile)
                    }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(painter = painterResource(R.drawable.ic_content_copy), contentDescription = "Copy preset link") },
                    text = { Text("Copy preset link") },
                    onClick = {
                        menuExpanded = false
                        onCopyLink(profile)
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    leadingIcon = { Icon(painter = painterResource(com.github.ishan09811.compose_preferences.R.drawable.ic_delete), contentDescription = "Delete") },
                    text = { Text("Delete") },
                    onClick = {
                        menuExpanded = false
                        onDelete(profile)
                    }
                )
            }
        }
    }
}

@Composable
fun ReceiverStatusChip(
    isActive: Boolean,
    expanded: Boolean,
    onExpandRequest: () -> Unit
) {
    val color = if (isActive) Color(0xFF4CAF50) else Color(0xFFF44336)
    val text = if (isActive) "Online" else "Offline"

    Surface(
        modifier = Modifier.clickable { onExpandRequest() },
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (expanded) 12.dp else 8.dp,
                vertical = 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            AnimatedVisibility(visible = expanded) {
                Row {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text,
                        color = color,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
