package io.github.controlbridge.ui.remap

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.controlbridge.models.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroEditorSheet(
    macro: Macro?,
    onSave: (Macro) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(macro?.name ?: "") }
    var steps by remember { mutableStateOf(macro?.steps?.toMutableList() ?: mutableListOf<MacroStep>()) }
    var showAddStep by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                if (macro != null) "Edit Macro" else "New Macro",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Macro Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            Text("Steps", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))

            if (steps.isEmpty()) {
                Text(
                    "No steps yet. Add steps below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Use a fixed-height container for the steps list
                Column {
                    steps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${index + 1}.",
                                modifier = Modifier.width(28.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                when (step) {
                                    is MacroStep.Press -> "Press ${step.key.name}"
                                    is MacroStep.Release -> "Release ${step.key.name}"
                                    is MacroStep.Delay -> "Delay ${step.millis}ms"
                                },
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            IconButton(
                                onClick = {
                                    steps = steps.toMutableList().also { it.removeAt(index) }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("✕", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Add step button
            OutlinedButton(
                onClick = { showAddStep = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Add Step")
            }

            Spacer(Modifier.height(24.dp))

            // Save button
            Button(
                onClick = {
                    if (name.isNotBlank() && steps.isNotEmpty()) {
                        onSave(
                            Macro(
                                id = macro?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                steps = steps.toList()
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && steps.isNotEmpty()
            ) {
                Text("Save Macro")
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // Add step dialog
    if (showAddStep) {
        AddStepDialog(
            onAdd = { step ->
                steps = steps.toMutableList().also { it.add(step) }
                showAddStep = false
            },
            onDismiss = { showAddStep = false }
        )
    }
}

@Composable
fun AddStepDialog(
    onAdd: (MacroStep) -> Unit,
    onDismiss: () -> Unit
) {
    var stepType by remember { mutableStateOf(0) } // 0=Press, 1=Release, 2=Delay
    var selectedKey by remember { mutableStateOf(GamepadKey.A) }
    var delayMs by remember { mutableStateOf("50") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Step") },
        text = {
            Column {
                // Step type tabs
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("Press", "Release", "Delay").forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = stepType == index,
                            onClick = { stepType = index },
                            shape = SegmentedButtonDefaults.itemShape(index, 3)
                        ) {
                            Text(label)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (stepType < 2) {
                    // Button selector
                    Text("Button", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    ButtonGrid(
                        selectedKeys = setOf(selectedKey),
                        onToggle = { selectedKey = it },
                        singleSelect = true
                    )
                } else {
                    // Delay input
                    OutlinedTextField(
                        value = delayMs,
                        onValueChange = { delayMs = it.filter { c -> c.isDigit() } },
                        label = { Text("Delay (ms)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val step = when (stepType) {
                    0 -> MacroStep.Press(selectedKey)
                    1 -> MacroStep.Release(selectedKey)
                    else -> MacroStep.Delay(delayMs.toLongOrNull() ?: 50)
                }
                onAdd(step)
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
