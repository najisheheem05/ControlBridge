package io.github.controlbridge.ui.remap

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.controlbridge.models.*

enum class ActionType { SINGLE_BUTTON, MULTI_BUTTON, MACRO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MappingEditorSheet(
    controlId: String,
    gesture: GestureType,
    currentMapping: Mapping?,
    availableMacros: List<Macro>,
    modes: List<Mode>,
    onSave: (Mapping?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Determine initial state from current mapping
    var actionType by remember {
        mutableStateOf(
            when (currentMapping?.action) {
                is MappingAction.ButtonPress -> ActionType.SINGLE_BUTTON
                is MappingAction.MultiButton -> ActionType.MULTI_BUTTON
                is MappingAction.TriggerMacro -> ActionType.MACRO
                else -> ActionType.SINGLE_BUTTON
            }
        )
    }

    var selectedKey by remember {
        mutableStateOf(
            (currentMapping?.action as? MappingAction.ButtonPress)?.key ?: GamepadKey.A
        )
    }

    var selectedKeys by remember {
        mutableStateOf(
            (currentMapping?.action as? MappingAction.MultiButton)?.keys?.toSet()
                ?: emptySet<GamepadKey>()
        )
    }

    var selectedMacroId by remember {
        mutableStateOf(
            (currentMapping?.action as? MappingAction.TriggerMacro)?.macroId ?: ""
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Edit: ${gesture.displayName()}",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(Modifier.height(16.dp))

            // Action type selector
            Text("Action Type", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ActionType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = actionType == type,
                        onClick = { actionType = type },
                        shape = SegmentedButtonDefaults.itemShape(index, ActionType.entries.size)
                    ) {
                        Text(
                            when (type) {
                                ActionType.SINGLE_BUTTON -> "Single"
                                ActionType.MULTI_BUTTON -> "Multi"
                                ActionType.MACRO -> "Macro"
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            when (actionType) {
                ActionType.SINGLE_BUTTON -> {
                    Text("Select Button", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    ButtonGrid(
                        selectedKeys = setOf(selectedKey),
                        onToggle = { key -> selectedKey = key },
                        singleSelect = true
                    )
                }

                ActionType.MULTI_BUTTON -> {
                    Text("Select Buttons (tap to toggle)", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    ButtonGrid(
                        selectedKeys = selectedKeys,
                        onToggle = { key ->
                            selectedKeys = if (key in selectedKeys) selectedKeys - key
                                           else selectedKeys + key
                        },
                        singleSelect = false
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Buttons stay held while your finger is on screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ActionType.MACRO -> {
                    if (availableMacros.isEmpty()) {
                        Text(
                            "No macros defined. Create macros first via Manage Macros.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text("Select Macro", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        availableMacros.forEach { macro ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedMacroId == macro.id,
                                    onClick = { selectedMacroId = macro.id }
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(macro.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        "${macro.steps.size} steps",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { onSave(null) }) {
                    Text("Clear")
                }
                Button(
                    onClick = {
                        val action = when (actionType) {
                            ActionType.SINGLE_BUTTON -> MappingAction.ButtonPress(selectedKey)
                            ActionType.MULTI_BUTTON -> {
                                if (selectedKeys.isEmpty()) return@Button
                                MappingAction.MultiButton(selectedKeys.toList())
                            }
                            ActionType.MACRO -> {
                                if (selectedMacroId.isEmpty()) return@Button
                                MappingAction.TriggerMacro(selectedMacroId)
                            }
                        }
                        onSave(Mapping(controlId = controlId, gesture = gesture, action = action))
                    }
                ) {
                    Text("Save")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun ButtonGrid(
    selectedKeys: Set<GamepadKey>,
    onToggle: (GamepadKey) -> Unit,
    singleSelect: Boolean
) {
    val rows = listOf(
        listOf(GamepadKey.A, GamepadKey.B, GamepadKey.X, GamepadKey.Y),
        listOf(GamepadKey.LB, GamepadKey.RB, GamepadKey.LT, GamepadKey.RT),
        listOf(GamepadKey.L3, GamepadKey.R3, GamepadKey.START, GamepadKey.SELECT)
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { key ->
                    val isSelected = key in selectedKeys
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggle(key) },
                        label = { Text(key.name) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
