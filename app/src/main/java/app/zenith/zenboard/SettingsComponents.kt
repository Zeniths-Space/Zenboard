package app.zenith.zenboard
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ColumnScope

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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

import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults

/* Shared building blocks for the settings, setup and about pages (Kontract-style grouped tiles). */

@Composable
internal fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 8.dp, top = 28.dp, bottom = 10.dp),
    )
}

/** Rounded segments stacked with small gaps (M3 Expressive grouped list). */
@Composable
internal fun SegmentedGroup(segments: List<@Composable () -> Unit>) {
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
internal fun StepTile(number: Int, title: String, subtitle: String, done: Boolean, onClick: (() -> Unit)?) {
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
internal fun GroupTile(
    badge: (@Composable () -> Unit)?,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null && trailing == null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (badge != null) {
            badge()
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = subtitleColor)
        }
        when {
            trailing != null -> {
                Spacer(Modifier.width(12.dp))
                trailing()
            }
            onClick != null -> {
                Spacer(Modifier.width(8.dp))
                Icon(KeyIcons.Next, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** A tile whose whole row toggles a switch (one touch target, announced as a switch). */
@Composable
internal fun SwitchTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    GroupTile(
        badge = { ScallopBadge(icon, ZenithColors.indicator, ZenithColors.onIndicator) },
        title = title,
        subtitle = subtitle,
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = cs.onSecondary,
                    checkedTrackColor = cs.secondary,
                    checkedBorderColor = cs.secondary,
                ),
            )
        },
    )
}

/** A category page: header with back button, then scrolling content. */
@Composable
internal fun CategoryPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding(),
    ) {
        PageTopBar(title, onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Sub-page header: back button and a large title. */
@Composable
internal fun PageTopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(KeyIcons.Previous, contentDescription = stringResource(R.string.back))
        }
        Spacer(Modifier.width(4.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}

/** Field tile: large rounded container with a scallop icon that turns solid on focus. */
@Composable
internal fun TryField() {
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
internal fun ScallopBadge(
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
