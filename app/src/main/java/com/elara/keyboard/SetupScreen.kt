package com.elara.keyboard

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Enable and select the keyboard, then try it. */
@Composable
fun SetupScreen(enabled: Boolean, selected: Boolean, onEnable: () -> Unit, onSelect: () -> Unit, onBack: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    // Signature action: only the next pending step, as a full-width pill pinned at the bottom.
    val action = when {
        !enabled -> stringResource(R.string.setup_open_settings) to onEnable
        !selected -> stringResource(R.string.setup_choose_keyboard) to onSelect
        else -> null
    }
    CategoryPage(
        stringResource(R.string.setup_title),
        onBack,
        bottomBar = {
            if (action != null) {
                Button(
                    onClick = action.second,
                    shape = StadiumShape,
                    colors = ButtonDefaults.buttonColors(containerColor = cs.secondary, contentColor = cs.onSecondary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .height(56.dp),
                ) { Text(action.first, style = MaterialTheme.typography.titleMedium) }
            }
        },
    ) {
        SectionHeader(stringResource(R.string.setup_section))
        SegmentedGroup(
            listOf(
                {
                    StepTile(
                        number = 1,
                        title = stringResource(R.string.setup_step_enable),
                        subtitle = stringResource(R.string.setup_step_enable_sub),
                        done = enabled,
                        onClick = onEnable,
                    )
                },
                {
                    StepTile(
                        number = 2,
                        title = stringResource(R.string.setup_step_select),
                        subtitle = stringResource(R.string.setup_step_select_sub),
                        done = selected,
                        onClick = if (enabled) onSelect else null,
                    )
                },
                {
                    GroupTile(
                        badge = { ScallopBadge(KeyIcons.Lock, cs.tertiaryContainer, cs.onTertiaryContainer) },
                        title = stringResource(R.string.setup_privacy_title),
                        subtitle = stringResource(R.string.setup_privacy_sub),
                    )
                },
            ),
        )
        Spacer(Modifier.height(24.dp))
    }
}
