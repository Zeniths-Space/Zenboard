package app.zenith.keyboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Launcher entry: walks the user through enabling and selecting the keyboard. */
class SetupActivity : ComponentActivity() {

    private var enabled by mutableStateOf(false)
    private var selected by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZenithTheme {
                SetupScreen(
                    enabled = enabled,
                    selected = selected,
                    onEnable = { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                    onSelect = { getSystemService(InputMethodManager::class.java).showInputMethodPicker() },
                )
            }
        }
    }

    // Re-check on focus: covers returning from system settings and closing the IME picker.
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) refresh()
    }

    private fun refresh() {
        val imm = getSystemService(InputMethodManager::class.java)
        enabled = imm.enabledInputMethodList.any { it.packageName == packageName }
        selected = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.startsWith("$packageName/") == true
    }
}

@Composable
private fun SetupScreen(enabled: Boolean, selected: Boolean, onEnable: () -> Unit, onSelect: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .background(cs.surface)
            .safeDrawingPadding(),
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(40.dp))
            ScallopBadge(KeyIcons.Keyboard, cs.secondary, cs.onSecondary, size = 88.dp)
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.setup_tagline), style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)

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

            SectionHeader(stringResource(R.string.setup_try_section))
            TryField()
            Spacer(Modifier.height(24.dp))
        }

        // Signature action: only the next pending step, as a full-width pill.
        val action = when {
            !enabled -> stringResource(R.string.setup_open_settings) to onEnable
            !selected -> stringResource(R.string.setup_choose_keyboard) to onSelect
            else -> null
        }
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
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 8.dp, top = 28.dp, bottom = 10.dp),
    )
}

/** Rounded segments stacked with small gaps (M3 Expressive grouped list). */
@Composable
private fun SegmentedGroup(segments: List<@Composable () -> Unit>) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        segments.forEachIndexed { i, segment ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = groupShape(i, segments.size),
                modifier = Modifier.fillMaxWidth(),
            ) { segment() }
        }
    }
}

@Composable
private fun StepTile(number: Int, title: String, subtitle: String, done: Boolean, onClick: (() -> Unit)?) {
    val cs = MaterialTheme.colorScheme
    val doneLabel = stringResource(R.string.setup_step_done)
    GroupTile(
        badge = {
            if (done) {
                ScallopBadge(KeyIcons.Done, cs.secondary, cs.onSecondary)
            } else {
                ScallopBadge(null, ZenithColors.indicator, ZenithColors.onIndicator, text = number.toString())
            }
        },
        title = title,
        subtitle = if (done) doneLabel else subtitle,
        subtitleColor = if (done) cs.secondary else cs.onSurfaceVariant,
        onClick = if (done) null else onClick,
        modifier = Modifier.semantics { stateDescription = if (done) doneLabel else "" },
    )
}

@Composable
private fun GroupTile(
    badge: @Composable () -> Unit,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        badge()
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = subtitleColor)
        }
        if (onClick != null) {
            Spacer(Modifier.width(8.dp))
            Icon(KeyIcons.Next, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

/** Field tile: large rounded container with a scallop icon that turns solid on focus. */
@Composable
private fun TryField() {
    val cs = MaterialTheme.colorScheme
    var text by rememberSaveable { mutableStateOf("") }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    // Keep the whole tile above the keyboard while it slides in (the field alone only reveals its cursor).
    val requester = remember { BringIntoViewRequester() }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(focused, imeBottom) { if (focused) requester.bringIntoView() }
    Row(
        Modifier
            .bringIntoViewRequester(requester)
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .background(cs.surfaceContainerHigh, RoundedCornerShape(28.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (focused) {
            ScallopBadge(KeyIcons.Keyboard, cs.secondary, cs.onSecondary)
        } else {
            ScallopBadge(KeyIcons.Keyboard, ZenithColors.indicator, ZenithColors.onIndicator)
        }
        Spacer(Modifier.width(16.dp))
        val placeholder = stringResource(R.string.setup_try_it)
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            interactionSource = interaction,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface),
            cursorBrush = SolidColor(cs.secondary),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (text.isEmpty()) {
                        Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
                    }
                    inner()
                }
            },
        )
    }
}

/** Scalloped badge with an icon or a short label. */
@Composable
private fun ScallopBadge(
    icon: ImageVector?,
    container: Color,
    content: Color,
    size: Dp = 44.dp,
    text: String? = null,
) {
    Box(Modifier.size(size).background(container, ScallopShape), contentAlignment = Alignment.Center) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(size * 0.46f))
        } else if (text != null) {
            Text(text, style = MaterialTheme.typography.titleMedium, color = content)
        }
    }
}
