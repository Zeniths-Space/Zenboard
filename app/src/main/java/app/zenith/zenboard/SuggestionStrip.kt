package app.zenith.zenboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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

enum class ToolbarAction { SELECT_ALL, CUT, COPY, PASTE, CLIPBOARD, SETTINGS }

/**
 * Fills the strip while no word is being typed: text editing and clipboard on the left, settings on
 * the right. Same height as the word strip, so the keyboard does not jump when one replaces the other.
 * Just after a copy, [freshClip] alone fills the row, as a one-tap paste sized to its text.
 */
@Composable
fun Toolbar(onAction: (ToolbarAction) -> Unit, freshClip: String?, onPasteFresh: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(42.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (freshClip != null) {
            // Only the copied text, centred, as wide as it needs (long text ends in an ellipsis).
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { FreshClip(freshClip, onPasteFresh, Modifier) }
            return@Row
        }
        ToolButton(KeyIcons.History, R.string.toolbar_clipboard) { onAction(ToolbarAction.CLIPBOARD) }
        ToolButton(KeyIcons.SelectAll, R.string.toolbar_select_all) { onAction(ToolbarAction.SELECT_ALL) }
        ToolButton(KeyIcons.Cut, R.string.toolbar_cut) { onAction(ToolbarAction.CUT) }
        ToolButton(KeyIcons.Copy, R.string.toolbar_copy) { onAction(ToolbarAction.COPY) }
        ToolButton(KeyIcons.Paste, R.string.toolbar_paste) { onAction(ToolbarAction.PASTE) }
        Spacer(Modifier.weight(1f))
        ToolButton(KeyIcons.Settings, R.string.toolbar_settings) { onAction(ToolbarAction.SETTINGS) }
    }
}

/** The text just copied, in a tertiary pill: tap to paste it. */
@Composable
private fun FreshClip(text: String, onPaste: () -> Unit, modifier: Modifier) {
    val feedback = LocalKeyFeedback.current
    val cs = MaterialTheme.colorScheme
    val label = stringResource(R.string.toolbar_paste_clip)
    Row(
        modifier
            .padding(horizontal = 4.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(cs.tertiary.copy(alpha = 0.8f))
            .clickable(role = Role.Button, onClickLabel = label) {
                feedback.press()
                onPaste()
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text.replace(Regex("\\s+"), " ").trim(),
            color = cs.onTertiary,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: Int, onClick: () -> Unit) {
    val feedback = LocalKeyFeedback.current
    Box(
        Modifier
            .size(width = 52.dp, height = 40.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(role = Role.Button) {
                feedback.press()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, stringResource(label), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

/** Three slots: alternative, best (centre, bold), alternative. An unknown typed word is shown in quotes. */
@Composable
fun SuggestionStrip(suggestions: Suggestions, onPick: (String) -> Unit) {
    val slots = listOf(suggestions.others.getOrNull(0), suggestions.primary, suggestions.others.getOrNull(1))
    Row(
        Modifier
            .fillMaxWidth()
            .height(42.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        slots.forEachIndexed { i, word ->
            if (i > 0) {
                Box(
                    Modifier
                        .width(1.dp)
                        .height(18.dp)
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
