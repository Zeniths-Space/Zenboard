package com.elara.keyboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Ready themes, each drawn as a small keyboard in its own colours; the chosen one has a ring. */
@Composable
internal fun ThemePicker(selected: KeyboardTheme, onPick: (KeyboardTheme) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val systemDark = isSystemInDarkTheme()
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KeyboardTheme.entries.forEach { theme ->
            val preview = remember(theme, systemDark) { schemeFor(theme.dark ?: systemDark, theme, 0) }
            val on = theme == selected
            Column(
                Modifier.selectable(on, role = Role.RadioButton) { onPick(theme) }.padding(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    Modifier
                        .size(width = 84.dp, height = 64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(preview.surface)
                        .border(BorderStroke(if (on) 3.dp else 1.dp, if (on) cs.primary else cs.outlineVariant), RoundedCornerShape(18.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    repeat(2) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(5) { Box(Modifier.weight(1f).size(height = 12.dp, width = 0.dp).clip(RoundedCornerShape(4.dp)).background(preview.surfaceContainerHigh)) }
                        }
                    }
                    Box(Modifier.fillMaxWidth().size(height = 12.dp, width = 0.dp).clip(RoundedCornerShape(6.dp)).background(preview.primary))
                }
                Text(
                    stringResource(theme.label),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (on) cs.primary else cs.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/** Accent colours over the chosen theme; the first circle keeps the theme's own. */
@Composable
internal fun AccentPicker(selected: Int, onPick: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        (listOf(0) + AccentChoices).forEach { accent ->
            val on = accent == selected
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (accent == 0) cs.surfaceContainerHigh else Color(accent))
                    .border(BorderStroke(if (on) 3.dp else 1.dp, if (on) cs.onSurface else cs.outlineVariant), CircleShape)
                    .selectable(on, role = Role.RadioButton) { onPick(accent) },
                contentAlignment = Alignment.Center,
            ) {
                if (accent == 0) Text(stringResource(R.string.accent_auto), style = MaterialTheme.typography.labelSmall, color = cs.onSurface)
                else if (on) Icon(KeyIcons.Done, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}
