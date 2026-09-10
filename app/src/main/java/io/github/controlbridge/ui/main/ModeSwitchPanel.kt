package io.github.controlbridge.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.controlbridge.models.Mode

@Composable
fun ModeSwitchPanel(
    isVisible: Boolean,
    modes: List<Mode>,
    activeModeId: String,
    onModeSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Scrim - tap to dismiss
        if (isVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable { onDismiss() }
            )
        }

        // Panel sliding from right
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight(0.85f)
                    .background(
                        Color.Black.copy(alpha = 0.88f),
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Modes",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                modes.forEach { mode ->
                    val isActive = mode.id == activeModeId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isActive) Color.White.copy(alpha = 0.15f)
                                else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                onModeSelected(mode.id)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (isActive) "●" else "○",
                            color = if (isActive) Color.Cyan else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            mode.name,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
