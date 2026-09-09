package io.github.controlbridge.ui.main

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
import io.github.controlbridge.profile.ProfileStorage
import io.github.controlbridge.viewmodel.GPEmulationViewModel
import kotlinx.coroutines.delay

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
    var showCreateDialog by remember { mutableStateOf(false) }

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
            }
        )
    }
}

@Composable
fun CreateProfileDialog(
    existingNames: Set<String>,
    onDismiss: () -> Unit,
    onCreate: (GameTemplate, String) -> Unit
) {
    var selectedTemplate by remember { mutableStateOf(GameTemplate.DEFAULT) }
    var profileName by remember {
        mutableStateOf(generateUniqueName(selectedTemplate.displayName, existingNames))
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
                                profileName = generateUniqueName(template.displayName, existingNames)
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
                                    profileName = generateUniqueName(template.displayName, existingNames)
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

private fun generateUniqueName(base: String, existing: Set<String>): String {
    if (base !in existing) return base
    var count = 2
    while ("$base ($count)" in existing) {
        count++
    }
    return "$base ($count)"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileCard(
    profile: Profile,
    onClick: (Profile) -> Unit,
    onEdit: (Profile) -> Unit,
    onRemap: (Profile) -> Unit,
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
