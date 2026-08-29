/*
 * Copyright (C) 2026 Ishan
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, version 3 only.
 *
 * This program is distributed without any warranty. See the GNU General Public License for more details.
 */


package io.github.controlbridge.ui.main

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.visible
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.materialswitch.MaterialSwitch
import io.github.controlbridge.R
import io.github.controlbridge.dialogs.AlertDialogQueue
import io.github.controlbridge.dialogs.AppDialog
import io.github.controlbridge.engine.MacroEngine
import io.github.controlbridge.engine.MappingEngine
import io.github.controlbridge.input.GestureRecognizer
import io.github.controlbridge.models.AnalogStickElement
import io.github.controlbridge.models.ButtonElement
import io.github.controlbridge.models.ControllerElement
import io.github.controlbridge.models.GestureType
import io.github.controlbridge.models.MappingAction
import io.github.controlbridge.models.Mode
import io.github.controlbridge.models.Profile
import io.github.controlbridge.profile.ProfileStorage
import io.github.controlbridge.profile.ProfileStorage.activeMode
import io.github.controlbridge.profile.ProfileStorage.updateElement
import io.github.controlbridge.transport.TransportManager
import io.github.controlbridge.utils.settings.GlobalConfig
import io.github.controlbridge.viewmodel.GPEmulationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun GPEmulationScreen(
    profile: Profile,
    viewModel: GPEmulationViewModel,
    isEditMode: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentProfile by remember { mutableStateOf(profile) }
    var activeModeId by remember { mutableStateOf(profile.activeModeId) }
    val activeMode = remember(activeModeId, currentProfile) {
        currentProfile.modes.find { it.id == activeModeId } ?: currentProfile.modes.first()
    }

    val defaultProfile = remember { ProfileStorage.createDefaultProfile(profile.name) }

    val controlPointers = remember { mutableSetOf<PointerId>() }
    val buttonBounds = remember { mutableStateMapOf<ButtonElement, Rect>() }

    // Gesture tracking state per pointer
    data class TouchTrack(
        val startPos: Offset,
        val startTimeMs: Long,
        val controlId: String,
        var activeAction: MappingEngine.ActiveAction? = null,
        var currentGesture: GestureType = GestureType.TAP,
        var holdTimerJob: Job? = null
    )
    val pointerTracks = remember { mutableStateMapOf<PointerId, TouchTrack>() }
    val swipeHoldDelayMs by GlobalConfig.swipeHoldDelayFlow.collectAsState(100)
    val swipeDistanceThreshold by GlobalConfig.swipeDistanceThresholdFlow.collectAsState(25)
    val gestureRecognizer = remember(swipeDistanceThreshold) { GestureRecognizer(swipeDistanceThreshold.toFloat()) }
    val macroEngine = remember { MacroEngine(scope) }
    val mappingEngine = remember {
        MappingEngine(macroEngine, scope).also { engine ->
            engine.onModeSwitchRequest = { modeId ->
                activeModeId = modeId
            }
        }
    }

    var selectedElementId by remember { mutableStateOf<String?>(null) }

    val selectedElement = remember(selectedElementId, currentProfile) {
        currentProfile.layout.elements.firstOrNull { it.id == selectedElementId }
    }

    // Mode switch panel
    var showModeSwitchPanel by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, isEditMode, currentProfile) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && isEditMode) {
                val profiles = ProfileStorage.load(context)
                val index = profiles.indexOfFirst { it.id == currentProfile.id }
                if (index != -1) {
                    profiles[index] = currentProfile
                    ProfileStorage.save(context, profiles)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val showLatencyIndicator by GlobalConfig.showLatencyFlow.collectAsState(true)

    FullScreen()
    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(isEditMode) {
                    if (isEditMode) {
                        detectTapGestures(
                            onTap = { selectedElementId = null }
                        )
                    }
                }
                .pointerInput(activeMode, swipeHoldDelayMs, gestureRecognizer) {
                    awaitPointerEventScope {
                        var cameraPointer: PointerId? = null
                        var lastPos = Offset.Zero
                        var currentVelocityX = 0f
                        var currentVelocityY = 0f
                        val sensitivity = 0.02f

                        while (true) {
                            if (isEditMode) return@awaitPointerEventScope
                            val event = awaitPointerEvent()

                            event.changes.forEach { change ->
                                if (change.changedToDown()) {
                                    val hit = buttonBounds.entries
                                        .firstOrNull { it.value.contains(change.position) }
                                        ?.key

                                    if (hit != null && hit.enabled) {
                                        val hasSwipeMappings = activeMode.mappings.any {
                                            it.controlId == hit.id && (
                                                it.gesture == GestureType.SWIPE_UP ||
                                                it.gesture == GestureType.SWIPE_DOWN ||
                                                it.gesture == GestureType.SWIPE_LEFT ||
                                                it.gesture == GestureType.SWIPE_RIGHT
                                            )
                                        }

                                        val tapMapping = activeMode.mappings.find {
                                            it.controlId == hit.id && it.gesture == GestureType.TAP
                                        }
                                        val tapAction = tapMapping?.action ?: MappingAction.ButtonPress(hit.key)

                                        var initialAction: MappingEngine.ActiveAction? = null
                                        var holdJob: Job? = null

                                        if (!hasSwipeMappings) {
                                            // No swipe mappings: activate immediately at t = 0ms!
                                            initialAction = mappingEngine.startAction(tapAction, activeMode, viewModel.transport)
                                        } else {
                                            // Has swipe mappings: start configurable micro-hold timer for real-time power charge
                                            val currentDelay = swipeHoldDelayMs.toLong()
                                            holdJob = scope.launch(Dispatchers.Default) {
                                                delay(currentDelay)
                                                val track = pointerTracks[change.id] ?: return@launch
                                                if (track.activeAction == null) {
                                                    track.activeAction = mappingEngine.startAction(tapAction, activeMode, viewModel.transport)
                                                }
                                            }
                                        }

                                        pointerTracks[change.id] = TouchTrack(
                                            startPos = change.position,
                                            startTimeMs = System.currentTimeMillis(),
                                            controlId = hit.id,
                                            activeAction = initialAction,
                                            currentGesture = GestureType.TAP,
                                            holdTimerJob = holdJob
                                        )
                                        controlPointers.add(change.id)
                                        return@forEach
                                    }
                                }

                                // Track ongoing button touches — detect swipe mid-drag
                                if (change.pressed && pointerTracks.containsKey(change.id)) {
                                    val track = pointerTracks[change.id] ?: return@forEach

                                    val detectedSwipe = gestureRecognizer.checkSwipe(track.startPos, change.position)
                                    if (detectedSwipe != null && detectedSwipe != track.currentGesture) {
                                        // Swipe detected! Cancel the hold timer so tap is never fired
                                        track.holdTimerJob?.cancel()
                                        track.holdTimerJob = null

                                        val swipeMapping = activeMode.mappings.find {
                                            it.controlId == track.controlId && it.gesture == detectedSwipe
                                        }
                                        if (swipeMapping != null) {
                                            track.activeAction?.release()
                                            track.activeAction = mappingEngine.startAction(
                                                swipeMapping.action,
                                                activeMode,
                                                viewModel.transport
                                            )
                                            track.currentGesture = detectedSwipe
                                        }
                                    }
                                    return@forEach
                                }

                                if (change.changedToUp()) {
                                    val track = pointerTracks.remove(change.id)
                                    if (track != null) {
                                        controlPointers.remove(change.id)
                                        track.holdTimerJob?.cancel()

                                        if (track.activeAction != null) {
                                            // Action was active (swipe or 50ms hold) — release it
                                            val durationMs = System.currentTimeMillis() - track.startTimeMs
                                            if (durationMs < 50) {
                                                val actionToRelease = track.activeAction
                                                scope.launch(Dispatchers.Default) {
                                                    delay(50 - durationMs)
                                                    actionToRelease?.release()
                                                }
                                            } else {
                                                track.activeAction?.release()
                                            }
                                        } else {
                                            // Released before 50ms without swiping — fire quick tap pulse (60ms)
                                            val tapMapping = activeMode.mappings.find {
                                                it.controlId == track.controlId && it.gesture == GestureType.TAP
                                            }
                                            val tapAction = tapMapping?.action
                                                ?: MappingAction.ButtonPress(
                                                    buttonBounds.keys.find { it.id == track.controlId }?.key
                                                        ?: return@forEach
                                                )
                                            val active = mappingEngine.startAction(
                                                tapAction, activeMode, viewModel.transport
                                            )
                                            scope.launch(Dispatchers.Default) {
                                                delay(60)
                                                active.release()
                                            }
                                        }
                                        return@forEach
                                    }
                                }

                                // Camera look (right stick) - unclaimed touches
                                if (cameraPointer == null && change.pressed
                                    && !controlPointers.contains(change.id)
                                    && !pointerTracks.containsKey(change.id)
                                ) {
                                    cameraPointer = change.id
                                    lastPos = change.position
                                }

                                if (change.id == cameraPointer && change.pressed) {
                                    val delta = change.position - lastPos
                                    lastPos = change.position

                                    currentVelocityX += delta.x * sensitivity
                                    currentVelocityY -= delta.y * sensitivity

                                    currentVelocityX = currentVelocityX.coerceIn(-1f, 1f)
                                    currentVelocityY = currentVelocityY.coerceIn(-1f, 1f)

                                    viewModel.transport?.setRightAxis(
                                        currentVelocityX,
                                        currentVelocityY
                                    )
                                }

                                if (change.id == cameraPointer && !change.pressed) {
                                    cameraPointer = null
                                    currentVelocityX = 0f
                                    currentVelocityY = 0f
                                    viewModel.transport?.setRightAxis(0f, 0f)
                                }
                            }
                        }
                    }
                }
        ) {
            if (showLatencyIndicator) LatencyIndicator(
                viewModel,
                modifier = Modifier.align(Alignment.TopStart)
            )

            // Mode guide / legend overlay
            if (!isEditMode && !activeMode.guideText.isNullOrBlank()) {
                ModeGuideOverlay(
                    guideText = activeMode.guideText,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 28.dp, start = 16.dp)
                )
            }

            // Mode indicator — tap to open mode switch panel
            if (!isEditMode && currentProfile.modes.size > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 16.dp)
                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .clickable { showModeSwitchPanel = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "▾ ${activeMode.name}",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            currentProfile.layout.elements.forEach { element ->
                when (element) {
                    is ButtonElement -> GamepadButton(
                        modifier = Modifier,
                        button = element,
                        screenWidth = maxWidth,
                        screenHeight = maxHeight,
                        buttonBounds = buttonBounds,
                        isPressed = pointerTracks.values.any { it.controlId == element.id },
                        isEditMode = isEditMode,
                        isSelected = selectedElementId == element.id,
                        onSelect = { selectedElementId = element.id },
                        onUpdate = { updated ->
                            currentProfile = currentProfile.updateElement(updated.id) { updated }
                        }
                    )

                    is AnalogStickElement -> AnalogStick(
                        dpad = element,
                        transport = viewModel.transport,
                        screenWidth = maxWidth,
                        screenHeight = maxHeight,
                        controlPointers = controlPointers,
                        isEditMode = isEditMode,
                        isSelected = selectedElementId == element.id,
                        onSelect = { selectedElementId = element.id },
                        onUpdate = { updated ->
                            currentProfile = currentProfile.updateElement(updated.id) { updated }
                        }
                    )
                }
            }

            if (isEditMode) {
                EditPanel(
                    element = selectedElement,
                    allElements = currentProfile.layout.elements,
                    onUpdate = { updated ->
                        currentProfile = currentProfile.updateElement(updated.id) { updated }
                    },
                    onUpdateAll = { size, opacity, enabled ->
                        var temp = currentProfile
                        currentProfile.layout.elements.forEach { el ->
                            val updated = when (el) {
                                is ButtonElement -> el.copy(
                                    size = size ?: el.size,
                                    opacity = opacity ?: el.opacity,
                                    enabled = enabled ?: el.enabled
                                )
                                is AnalogStickElement -> el.copy(
                                    size = size ?: el.size,
                                    opacity = opacity ?: el.opacity,
                                    enabled = enabled ?: el.enabled
                                )
                                else -> el
                            }
                            temp = temp.updateElement(updated.id) { updated }
                        }
                        currentProfile = temp
                    },
                    onReset = {
                        AlertDialogQueue.show(
                            AppDialog.Message(
                                title = "Reset Button: ${selectedElement?.id ?: "All"}",
                                message = "Are you sure you want to reset this button?",
                                onConfirm = {
                                    if (selectedElementId != null) {
                                        val defaultElement =
                                            defaultProfile.layout.elements.firstOrNull { it.id == selectedElementId }
                                        if (defaultElement != null) {
                                            currentProfile = currentProfile.updateElement(selectedElementId!!) { defaultElement }
                                        }
                                    } else {
                                        currentProfile = currentProfile.copy(layout = defaultProfile.layout)
                                    }
                                }
                            )
                        )
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // Mode switch panel overlay
        if (!isEditMode) {
            ModeSwitchPanel(
                isVisible = showModeSwitchPanel,
                modes = currentProfile.modes,
                activeModeId = activeModeId,
                onModeSelected = { modeId -> activeModeId = modeId },
                onDismiss = { showModeSwitchPanel = false }
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GamepadButton(
    modifier: Modifier,
    button: ButtonElement,
    screenWidth: Dp,
    screenHeight: Dp,
    buttonBounds: MutableMap<ButtonElement, Rect>,
    isPressed: Boolean = false,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onUpdate: (ButtonElement) -> Unit,
    isEditMode: Boolean
) {
    val density = LocalDensity.current

    val screenWidthPx = with(density) { screenWidth.toPx() }
    val screenHeightPx = with(density) { screenHeight.toPx() }

    val latestButton by rememberUpdatedState(button)

    var localX by remember(button.id) {
        mutableFloatStateOf(button.x)
    }

    var localY by remember(button.id) {
        mutableFloatStateOf(button.y)
    }

    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(button.x, button.y) {
        if (!isDragging) {
            localX = button.x
            localY = button.y
        }
    }

    val sizeDp = screenWidth * button.size
    val sizePx = screenWidthPx * button.size

    val xPx = screenWidthPx * localX - sizePx / 2f
    val yPx = screenHeightPx * localY - sizePx / 2f

    buttonBounds[button] = Rect(
        xPx,
        yPx,
        xPx + sizePx,
        yPx + sizePx
    )

    Box(
        modifier = modifier
            .offset(
                x = screenWidth * localX - sizeDp / 2,
                y = screenHeight * localY - sizeDp / 2
            )
            .size(sizeDp)
            .graphicsLayer {
                alpha = button.opacity
            }
            .background(
                when {
                    !button.enabled -> Color.White.copy(alpha = 0.05f)
                    !isPressed -> Color.White.copy(alpha = 0.3f)
                    else -> Color.Transparent
                },
                CircleShape
            )
            .border(
                width = when {
                    isSelected -> 2.dp
                    !button.enabled -> 1.dp
                    else -> 0.dp
                },

                color = when {
                    isSelected -> Color.Cyan
                    !button.enabled -> Color.White.copy(alpha = 0.25f)
                    else -> Color.Transparent
                },

                shape = CircleShape
            )
            .visible(if (!isEditMode) button.enabled else true)
            .pointerInput(isEditMode) {
                if (!isEditMode) return@pointerInput

                detectTapGestures(
                    onTap = {
                        onSelect()
                    }
                )
            }
            .pointerInput(isEditMode, button.enabled) {
                if (!isEditMode || !button.enabled) return@pointerInput

                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        onSelect()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        localX = (localX + dragAmount.x / screenWidthPx).coerceIn(0f, 1f)
                        localY = (localY + dragAmount.y / screenHeightPx).coerceIn(0f, 1f)
                    },

                    onDragEnd = {
                        isDragging = false
                        onUpdate(
                            latestButton.copy(
                                x = localX,
                                y = localY
                            )
                        )
                    },
                    onDragCancel = {
                        isDragging = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                alpha = if (button.enabled) button.opacity else 0.35f
            }
        ) {
            GamepadButtonLabel(button.key.name)
        }
    }
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AnalogStick(
    dpad: AnalogStickElement,
    transport: TransportManager?,
    screenWidth: Dp,
    screenHeight: Dp,
    controlPointers: MutableSet<PointerId>,
    isEditMode: Boolean = false,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
    onUpdate: (AnalogStickElement) -> Unit = {}
) {
    val density = LocalDensity.current
    val screenWidthPx = with(density) { screenWidth.toPx() }
    val screenHeightPx = with(density) { screenHeight.toPx() }

    val sizeDp = screenWidth * dpad.size
    val sizePx = screenWidthPx * dpad.size
    val radius = sizePx / 2f

    val latestDpad by rememberUpdatedState(dpad)

    var localX by remember(dpad.id) { mutableFloatStateOf(dpad.x) }
    var localY by remember(dpad.id) { mutableFloatStateOf(dpad.y) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(dpad.x, dpad.y) {
        if (!isDragging) {
            localX = dpad.x
            localY = dpad.y
        }
    }

    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    var activePointer by remember { mutableStateOf<PointerId?>(null) }

    Box(
        modifier = Modifier
            .offset(
                x = screenWidth * localX - sizeDp / 2,
                y = screenHeight * localY - sizeDp / 2
            )
            .size(sizeDp)
            .graphicsLayer { alpha = dpad.opacity }
            .background(
                when {
                    !dpad.enabled -> Color.White.copy(alpha = 0.05f)
                    else -> Color.Transparent
                },
                CircleShape
            )
            .border(
                width = when {
                    isSelected -> 2.dp
                    !dpad.enabled -> 1.dp
                    else -> 0.dp
                },
                color = when {
                    isSelected -> Color.Cyan
                    !dpad.enabled -> Color.White.copy(alpha = 0.25f)
                    else -> Color.Transparent
                },
                shape = CircleShape
            )
            .visible(if (!isEditMode) dpad.enabled else true)
            .pointerInput(isEditMode) {
                if (!isEditMode) return@pointerInput
                detectTapGestures(
                    onTap = { onSelect() }
                )
            }
            .pointerInput(isEditMode, dpad.enabled) {
                if (!isEditMode || !dpad.enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        onSelect()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        localX = (localX + dragAmount.x / screenWidthPx).coerceIn(0f, 1f)
                        localY = (localY + dragAmount.y / screenHeightPx).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        isDragging = false
                        onUpdate(
                            latestDpad.copy(
                                x = localX,
                                y = localY
                            )
                        )
                    },
                    onDragCancel = {
                        isDragging = false
                    }
                )
            }
            .pointerInput(Unit) {
                if (isEditMode) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()

                        event.changes.forEach { change ->
                            if (change.pressed && activePointer == null) {
                                activePointer = change.id
                                controlPointers.add(change.id)
                            }

                            if (change.id == activePointer && change.pressed) {
                                val center = Offset(radius, radius)
                                val delta = change.position - center

                                val dist = delta.getDistance()
                                val clamped =
                                    if (dist > radius) delta * (radius / dist)
                                    else delta

                                knobOffset = clamped

                                val x = (clamped.x / radius).coerceIn(-1f, 1f)
                                val y = (-clamped.y / radius).coerceIn(-1f, 1f)

                                transport?.setLeftAxis(x, y)
                            }

                            if (change.id == activePointer && change.changedToUp()) {
                                activePointer = null
                                controlPointers.remove(change.id)
                                knobOffset = Offset.Zero

                                transport?.setLeftAxis(0f, 0f)
                            }
                        }
                    }
                }
            }
    ) {
        AnalogStickVisual(knobOffset)
    }
}

@Composable
private fun AnalogStickVisual(knobOffset: Offset) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color.White.copy(alpha = 0.15f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Thumb
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        knobOffset.x.roundToInt(),
                        knobOffset.y.roundToInt()
                    )
                }
                .size(28.dp)
                .background(
                    Color.White.copy(alpha = 0.5f),
                    CircleShape
                )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPanel(
    element: ControllerElement?,
    allElements: List<ControllerElement>,
    onUpdate: (ControllerElement) -> Unit,
    onUpdateAll: (size: Float?, opacity: Float?, enabled: Boolean?) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember {
        mutableFloatStateOf(40f)
    }

    var offsetY by remember {
        mutableFloatStateOf(120f)
    }

    val isAllSelected = element == null
    val referenceElement = element ?: allElements.firstOrNull()

    val currentSize = referenceElement?.size ?: 0.15f
    val currentOpacity = referenceElement?.opacity ?: 0.5f
    val currentEnabled = referenceElement?.enabled ?: true

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    offsetX.roundToInt(),
                    offsetY.roundToInt()
                )
            }
            .width(260.dp)
            .background(
                Color.Black.copy(alpha = 0.92f),
                RoundedCornerShape(20.dp)
            )
            .border(
                1.dp,
                Color.White.copy(alpha = 0.15f),
                RoundedCornerShape(20.dp)
            )
    ) {

        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp
                        )
                    )
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()

                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = element?.id ?: "All",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onReset,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painterResource(R.drawable.ic_refresh),
                        contentDescription = "Reset Customizations",
                        tint = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            Color.White.copy(alpha = 0.08f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "≡",
                        color = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Size",
                    color = Color.White
                )

                Slider(
                    value = currentSize,
                    onValueChange = { value ->
                        if (isAllSelected) {
                            onUpdateAll(value, null, null)
                        } else {
                            element.let {
                                when (it) {
                                    is ButtonElement -> onUpdate(it.copy(size = value))
                                    is AnalogStickElement -> onUpdate(it.copy(size = value))
                                }
                            }
                        }
                    },
                    valueRange = 0.05f..0.3f,
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                        )
                    }
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Opacity",
                    color = Color.White
                )

                Slider(
                    value = currentOpacity,
                    onValueChange = { value ->
                        if (isAllSelected) {
                            onUpdateAll(null, value, null)
                        } else {
                            element.let {
                                when (it) {
                                    is ButtonElement -> onUpdate(it.copy(opacity = value))
                                    is AnalogStickElement -> onUpdate(it.copy(opacity = value))
                                }
                            }
                        }
                    },
                    valueRange = 0.1f..1f,
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                        )
                    }
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Enabled",
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )

                    MaterialSwitch(
                        checked = currentEnabled,
                        onCheckedChange = { enabled ->
                            if (isAllSelected) {
                                onUpdateAll(null, null, enabled)
                            } else {
                                element.let {
                                    when (it) {
                                        is ButtonElement -> onUpdate(it.copy(enabled = enabled))
                                        is AnalogStickElement -> onUpdate(it.copy(enabled = enabled))
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun FullScreen() {
    val context = LocalContext.current
    val window = (context as Activity).window
    val controller = remember {
        WindowCompat.getInsetsController(window, window.decorView)
    }

    DisposableEffect(Unit) {
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
fun GamepadButtonLabel(keyName: String) {
    when (keyName) {
        "A" -> Text("A", style = labelStyle(), color = Color.White)
        "B" -> Text("B", style = labelStyle(), color = Color.White)
        "X" -> Text("X", style = labelStyle(), color = Color.White)
        "Y" -> Text("Y", style = labelStyle(), color = Color.White)

        "LB" -> Text("LB", style = smallLabelStyle(), color = Color.White)
        "RB" -> Text("RB", style = smallLabelStyle(), color = Color.White)

        "START" -> Icon(
            painter = painterResource(R.drawable.ic_play_arrow),
            tint = Color.White,
            contentDescription = "Start"
        )

        "SELECT" -> Icon(
            painter = painterResource(R.drawable.ic_menu),
            tint = Color.White,
            contentDescription = "Select"
        )

        else -> Text(keyName, style = smallLabelStyle(), color = Color.White)
    }
}

@Composable
private fun LatencyIndicator(viewModel: GPEmulationViewModel, modifier: Modifier = Modifier) {
    val lastLatency by viewModel.lastLatency.collectAsState()
    Text(
        text = if (lastLatency != null) String.format("%.1f ms", lastLatency) else "",
        modifier = modifier.padding(start = 25.dp),
        color = Color.White
    )
}

@Composable
private fun labelStyle() = MaterialTheme.typography.titleLarge.copy(
    fontWeight = FontWeight.Bold
)

@Composable
private fun smallLabelStyle() = MaterialTheme.typography.labelMedium

@Composable
fun ModeGuideOverlay(
    guideText: String,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }

    if (isVisible) {
        Box(
            modifier = modifier
                .background(
                    Color.Black.copy(alpha = 0.45f),
                    RoundedCornerShape(8.dp)
                )
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.12f),
                    RoundedCornerShape(8.dp)
                )
                .clickable { isVisible = false }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = guideText,
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            )
        }
    } else {
        Box(
            modifier = modifier
                .background(
                    Color.Black.copy(alpha = 0.35f),
                    RoundedCornerShape(6.dp)
                )
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.10f),
                    RoundedCornerShape(6.dp)
                )
                .clickable { isVisible = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Guide ▾",
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
