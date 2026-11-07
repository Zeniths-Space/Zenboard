package com.elara.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Height of the toolbar and the word strip, shared so swapping one for the other never moves the keys. */
private val StripHeight = 36.dp

enum class ToolbarAction { SELECT_ALL, CUT, COPY, PASTE, CLIPBOARD, ONE_HANDED, SETTINGS }

/**
 * Fills the strip while no word is being typed: clipboard, editing actions and settings, spread
 * evenly over the row. Same height as the word strip, so the keyboard does not jump when one
 * replaces the other.
 * Just after a copy, [freshClip] alone fills the row, as a one-tap paste sized to its text.
 */
@Composable
fun Toolbar(
    onAction: (ToolbarAction) -> Unit,
    freshClip: String?,
    onPasteFresh: () -> Unit,
    clipboardOpen: Boolean,
    oneHandedOn: Boolean,
    buttons: List<ToolbarAction>,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(StripHeight)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (freshClip != null) {
            // Only the copied text, centred, as wide as it needs up to a cap (then an ellipsis).
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { FreshClip(freshClip, onPasteFresh, Modifier) }
            return@Row
        }
        // Each button gets an equal share of the row, so the touch targets fill it with no dead gap.
        val slot = Modifier.weight(1f)
        // The chosen buttons (Look & feel > Toolbar), then settings, always last.
        (buttons + ToolbarAction.SETTINGS).forEach { action ->
            // Clipboard and one-handed are lit while on; a second tap turns them off.
            val selected = (action == ToolbarAction.CLIPBOARD && clipboardOpen) || (action == ToolbarAction.ONE_HANDED && oneHandedOn)
            ToolButton(toolIcon(action), toolLabel(action), slot, selected = selected) { onAction(action) }
        }
    }
}

fun toolIcon(action: ToolbarAction) = when (action) {
    ToolbarAction.CLIPBOARD -> KeyIcons.Clipboard
    ToolbarAction.SELECT_ALL -> KeyIcons.SelectAll
    ToolbarAction.CUT -> KeyIcons.Cut
    ToolbarAction.COPY -> KeyIcons.Copy
    ToolbarAction.PASTE -> KeyIcons.Paste
    ToolbarAction.ONE_HANDED -> KeyIcons.OneHanded
    ToolbarAction.SETTINGS -> KeyIcons.Settings
}

fun toolLabel(action: ToolbarAction) = when (action) {
    ToolbarAction.CLIPBOARD -> R.string.toolbar_clipboard
    ToolbarAction.SELECT_ALL -> R.string.toolbar_select_all
    ToolbarAction.CUT -> R.string.toolbar_cut
    ToolbarAction.COPY -> R.string.toolbar_copy
    ToolbarAction.PASTE -> R.string.toolbar_paste
    ToolbarAction.ONE_HANDED -> R.string.toolbar_one_handed
    ToolbarAction.SETTINGS -> R.string.toolbar_settings
}

/** The text just copied, in a glassy outlined pill: tap to paste it. */
@Composable
private fun FreshClip(text: String, onPaste: () -> Unit, modifier: Modifier) {
    val feedback = LocalKeyFeedback.current
    val cs = MaterialTheme.colorScheme
    val label = stringResource(R.string.toolbar_paste_clip)
    Row(
        modifier
            .padding(horizontal = 4.dp)
            // Long copies stop at about half the row and end in an ellipsis.
            .widthIn(max = 200.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            // A dark glassy pill: faint primary-to-tertiary tint inside, a thin light rim that
            // shifts colour along its length.
            .background(
                Brush.horizontalGradient(listOf(cs.primary.copy(alpha = 0.16f), cs.tertiary.copy(alpha = 0.12f))),
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(cs.primary.copy(alpha = 0.55f), cs.onSurface.copy(alpha = 0.35f), cs.tertiary.copy(alpha = 0.55f)),
                ),
                RoundedCornerShape(15.dp),
            )
            .clickable(role = Role.Button, onClickLabel = label) {
                feedback.press()
                onPaste()
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text.replace(Regex("\\s+"), " ").trim(),
            // The pill is see-through, so the text follows the surface behind it.
            color = cs.onSurface,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: Int, modifier: Modifier, selected: Boolean = false, onClick: () -> Unit) {
    val feedback = LocalKeyFeedback.current
    Box(
        modifier
            .height(34.dp)
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (selected) ZenithColors.indicator else Color.Transparent)
            .semantics { this.selected = selected }
            .clickable(role = Role.Button) {
                feedback.press()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            stringResource(label),
            tint = if (selected) ZenithColors.onIndicator else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(19.dp),
        )
    }
}

/**
 * The suggestion row with no word to suggest for (toolbar off): a faint handle, so the row reads as
 * part of the keyboard rather than empty space. It keeps the row's height, so words can appear
 * without resizing the keyboard.
 */
@Composable
fun IdleStrip() {
    Box(Modifier.fillMaxWidth().height(StripHeight), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(width = 36.dp, height = 4.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(2.dp)),
        )
    }
}

/** Three slots: alternative, best (centre, bold), alternative. An unknown typed word is shown in quotes. */
@Composable
fun SuggestionStrip(suggestions: Suggestions, onPick: (String) -> Unit) {
    val slots = listOf(suggestions.others.getOrNull(0), suggestions.primary, suggestions.others.getOrNull(1))
    Row(
        Modifier
            .fillMaxWidth()
            .height(StripHeight)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        slots.forEachIndexed { i, word ->
            if (i > 0) {
                Box(
                    Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (word != null && slots[i - 1] != null) 1f else 0f)),
                )
            }
            Slot(
                word = word,
                center = i == 1,
                willApply = i == 1 && word != null && word == suggestions.autocorrect,
                verbatim = suggestions.typedIsUnknown && i == 0 && word != null,
                onPick = onPick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Slot(word: String?, center: Boolean, willApply: Boolean, verbatim: Boolean, onPick: (String) -> Unit, modifier: Modifier) {
    val feedback = LocalKeyFeedback.current
    val cs = MaterialTheme.colorScheme
    Box(
        modifier
            .fillMaxHeight()
            .padding(vertical = 3.dp, horizontal = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (word != null) {
                    Modifier.clickable {
                        feedback.press()
                        onPick(word)
                    }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (word != null) {
            Text(
                if (verbatim) "“$word”" else word,
                // The accent marks the word space will type (autocorrect).
                color = when {
                    willApply -> cs.secondary
                    center -> cs.onSurface
                    else -> cs.onSurfaceVariant
                },
                fontSize = 16.sp,
                fontWeight = if (center) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
    }
}
