package com.elara.keyboard

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * Clipboard history in the space of the upper key rows: a two-column staggered grid, pinned clips
 * first, each card as tall as its text (up to four lines). Tap pastes; long-press offers pin or
 * delete. Clearing the whole history is in the settings screen.
 */
@Composable
fun ClipboardPanel(
    clips: List<Clip>,
    historyOn: Boolean,
    height: Dp,
    onPaste: (String) -> Unit,
    onPin: (Clip) -> Unit,
    onDelete: (Clip) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().height(height)) {
        if (clips.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(KeyIcons.Clipboard, contentDescription = null, tint = cs.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(if (historyOn) R.string.clipboard_empty else R.string.clipboard_off),
                    color = cs.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            // Read once per composition: ages refresh whenever the panel is shown, with no timer.
            val now = System.currentTimeMillis()
            LazyVerticalStaggeredGrid(
                StaggeredGridCells.Fixed(2),
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                verticalItemSpacing = 6.dp,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(clips, key = { it.text }) { clip -> ClipCard(clip, now, onPaste, onPin, onDelete) }
            }
        }
    }
}

/** Short age of a clip: "now", then minutes, hours, days. */
@Composable
private fun clipAge(now: Long, time: Long): String {
    val minutes = ((now - time) / 60_000L).coerceAtLeast(0)
    return when {
        minutes < 1 -> stringResource(R.string.clipboard_age_now)
        minutes < 60 -> stringResource(R.string.clipboard_age_minutes, minutes)
        minutes < 24 * 60 -> stringResource(R.string.clipboard_age_hours, minutes / 60)
        else -> stringResource(R.string.clipboard_age_days, minutes / (24 * 60))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClipCard(clip: Clip, now: Long, onPaste: (String) -> Unit, onPin: (Clip) -> Unit, onDelete: (Clip) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val feedback = LocalKeyFeedback.current
    var menu by remember { mutableStateOf(false) }
    val pinLabel = stringResource(if (clip.pinned) R.string.clipboard_unpin else R.string.clipboard_pin)
    val deleteLabel = stringResource(R.string.clipboard_delete)
    val (container, content) = if (clip.pinned) cs.secondaryContainer to cs.onSecondaryContainer else cs.surfaceContainerHigh to cs.onSurface
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .combinedClickable(
                onClick = {
                    feedback.press()
                    onPaste(clip.text)
                },
                onLongClick = {
                    feedback.haptic(HapticFeedbackConstants.LONG_PRESS)
                    menu = true
                },
            )
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(pinLabel) { onPin(clip); true },
                    CustomAccessibilityAction(deleteLabel) { onDelete(clip); true },
                )
            }
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
    ) {
        Column {
            Text(
                // Keep line breaks, but not runs of blank lines or edge spaces.
                clip.text.trim().replace(Regex("\\n\\s*\\n+"), "\n"),
                color = content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(clipAge(now, clip.time), color = content.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                if (clip.pinned) {
                    Spacer(Modifier.weight(1f))
                    Icon(KeyIcons.Pin, contentDescription = null, tint = content.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                }
            }
        }
        if (menu) {
            Popup(popupPositionProvider = AboveAnchor, onDismissRequest = { menu = false }, properties = PopupProperties(clippingEnabled = false)) {
                Row(
                    Modifier
                        .shadow(6.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(cs.surfaceContainerHighest)
                        .padding(4.dp),
                ) {
                    MenuButton(KeyIcons.Pin, pinLabel) {
                        menu = false
                        onPin(clip)
                    }
                    MenuButton(KeyIcons.Close, deleteLabel) {
                        menu = false
                        onDelete(clip)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    val feedback = LocalKeyFeedback.current
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button) {
                feedback.press()
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
    }
}
