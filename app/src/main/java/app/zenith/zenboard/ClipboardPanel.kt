package app.zenith.zenboard

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * Clipboard history in the space of the upper key rows. Pinned clips first. Tap pastes; long-press
 * offers pin or delete. "Clear all" (unpinned) sits in the bottom row, like the emoji recents.
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
    Column(Modifier.fillMaxWidth().height(height)) {
        Row(Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(KeyIcons.History, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.clipboard_title), color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        }
        if (clips.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(if (historyOn) R.string.clipboard_empty else R.string.clipboard_off),
                    color = cs.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyVerticalGrid(
                GridCells.Fixed(2),
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(clips, key = { it.text }) { clip -> ClipCard(clip, onPaste, onPin, onDelete) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClipCard(clip: Clip, onPaste: (String) -> Unit, onPin: (Clip) -> Unit, onDelete: (Clip) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val feedback = LocalKeyFeedback.current
    var menu by remember { mutableStateOf(false) }
    val pinLabel = stringResource(if (clip.pinned) R.string.clipboard_unpin else R.string.clipboard_pin)
    val deleteLabel = stringResource(R.string.clipboard_delete)
    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cs.surfaceContainerHigh)
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
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            // Line breaks would waste the three lines on blank space.
            clip.text.replace(Regex("\\s+"), " ").trim(),
            color = cs.onSurface,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = if (clip.pinned) 16.dp else 0.dp),
        )
        if (clip.pinned) {
            Icon(KeyIcons.Pin, contentDescription = null, tint = cs.secondary, modifier = Modifier.size(14.dp).align(Alignment.TopEnd))
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
