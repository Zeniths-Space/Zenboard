package app.zenith.zenboard

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val view = LocalView.current
    val cs = MaterialTheme.colorScheme
    Box(
        modifier
            .fillMaxHeight()
            .padding(vertical = 3.dp, horizontal = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (word != null) {
                    Modifier.clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
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
