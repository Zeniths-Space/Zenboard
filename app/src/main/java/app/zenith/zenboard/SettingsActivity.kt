package app.zenith.zenboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class Page { HOME, SETUP, TYPING, KEYS, CLIPBOARD, LOOK, ABOUT }

/** Launcher entry and the keyboard's settings activity: settings home, setup and about pages. */
class SettingsActivity : ComponentActivity() {

    private var enabled by mutableStateOf(false)
    private var selected by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        setContent {
            val settings by SettingsStore.settings(this).collectAsState(initial = null)
            val current = settings ?: return@setContent // first frame waits for the (fast) settings read
            ZenithTheme(pureBlack = current.pureBlack) {
                var page by rememberSaveable { mutableStateOf(Page.HOME) }
                BackHandler(enabled = page != Page.HOME) { page = Page.HOME }
                val slide = with(LocalDensity.current) { 30.dp.roundToPx() }
                AnimatedContent(
                    targetState = page,
                    // Surface behind both pages: otherwise the grey window background flashes between them.
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                    // M3 shared axis X: both pages move together; old fades out fast, new fades in after it.
                    transitionSpec = {
                        val dir = if (targetState != Page.HOME) 1 else -1
                        (slideInHorizontally(Motion.spatial(400)) { dir * slide } + fadeIn(Motion.effects(250, delay = 90)))
                            .togetherWith(slideOutHorizontally(Motion.spatial(400)) { -dir * slide } + fadeOut(Motion.effects(90)))
                    },
                    label = "page",
                ) { target ->
                    when (target) {
                        Page.HOME -> WithTryKeyboard {
                            HomeScreen(
                                settings = current,
                                enabled = enabled,
                                selected = selected,
                                versionName = versionName,
                                onOpen = { page = it },
                            )
                        }
                        Page.SETUP -> SetupScreen(
                            enabled = enabled,
                            selected = selected,
                            onEnable = { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                            onSelect = { getSystemService(InputMethodManager::class.java).showInputMethodPicker() },
                            onBack = { page = Page.HOME },
                        )
                        Page.TYPING -> WithTryKeyboard { TypingScreen(current, onBack = { page = Page.HOME }) }
                        Page.KEYS -> WithTryKeyboard { KeysScreen(current, onBack = { page = Page.HOME }) }
                        Page.CLIPBOARD -> WithTryKeyboard { ClipboardScreen(current, onBack = { page = Page.HOME }) }
                        Page.LOOK -> WithTryKeyboard { LookScreen(current, onBack = { page = Page.HOME }) }
                        Page.ABOUT -> AboutScreen(versionName, onBack = { page = Page.HOME })
                    }
                }
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

/**
 * Settings home: a setup banner while the keyboard is not ready, a grid of four category cards,
 * then setup and about as small cards.
 */
@Composable
private fun HomeScreen(
    settings: ZenboardSettings,
    enabled: Boolean,
    selected: Boolean,
    versionName: String,
    onOpen: (Page) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val ready = enabled && selected
    CategoryPage(stringResource(R.string.app_name), onBack = null) {
        Text(
            stringResource(R.string.setup_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 20.dp),
        )
        if (!ready) {
            SetupBanner(enabled, onClick = { onOpen(Page.SETUP) })
            Spacer(Modifier.height(16.dp))
        }
        val gap = 12.dp
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            CategoryCard(
                KeyIcons.Words,
                stringResource(R.string.settings_section_typing),
                stringResource(R.string.settings_typing_card),
                cs.primaryContainer,
                cs.onPrimaryContainer,
                onClick = { onOpen(Page.TYPING) },
                modifier = Modifier.weight(1f),
            )
            CategoryCard(
                KeyIcons.Keyboard,
                stringResource(R.string.settings_section_keys),
                stringResource(R.string.settings_keys_card),
                cs.tertiaryContainer,
                cs.onTertiaryContainer,
                onClick = { onOpen(Page.KEYS) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(gap))
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            CategoryCard(
                KeyIcons.Clipboard,
                stringResource(R.string.settings_section_clipboard),
                if (settings.clipHistory) {
                    stringResource(R.string.settings_clipboard_summary_on, stringResource(retentionLabel(settings.clipRetention)).lowercase())
                } else {
                    stringResource(R.string.settings_clipboard_summary_off)
                },
                cs.secondaryContainer,
                cs.onSecondaryContainer,
                onClick = { onOpen(Page.CLIPBOARD) },
                modifier = Modifier.weight(1f),
            )
            CategoryCard(
                KeyIcons.DarkMode,
                stringResource(R.string.settings_section_look),
                stringResource(R.string.settings_look_card),
                cs.surfaceContainerHighest,
                cs.onSurface,
                onClick = { onOpen(Page.LOOK) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(gap))
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            SmallCard(
                if (ready) KeyIcons.Done else KeyIcons.Keyboard,
                stringResource(R.string.settings_section_setup),
                stringResource(if (ready) R.string.settings_setup_ready_short else R.string.settings_setup_needed_short),
                onClick = { onOpen(Page.SETUP) },
                modifier = Modifier.weight(1f),
            )
            SmallCard(
                KeyIcons.Info,
                stringResource(R.string.settings_section_about),
                stringResource(R.string.about_version, versionName),
                onClick = { onOpen(Page.ABOUT) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Shown only while the keyboard is not enabled or not selected: the one thing to do first. */
@Composable
private fun SetupBanner(enabled: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(onClick = onClick, shape = RoundedCornerShape(28.dp), color = cs.errorContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_setup_banner), style = MaterialTheme.typography.titleMedium, color = cs.onErrorContainer)
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(if (enabled) R.string.settings_setup_not_selected else R.string.settings_setup_not_enabled),
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onErrorContainer.copy(alpha = 0.8f),
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(KeyIcons.Next, contentDescription = null, tint = cs.onErrorContainer)
        }
    }
}

/** Words, capitals and spacing. */
@Composable
private fun TypingScreen(settings: ZenboardSettings, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    CategoryPage(stringResource(R.string.settings_section_typing), onBack) {
        SectionHeader(stringResource(R.string.settings_typing_words))
        SegmentedGroup(
            listOf<@Composable () -> Unit>(
                {
                    SwitchTile(KeyIcons.Words, stringResource(R.string.settings_suggestions), stringResource(R.string.settings_suggestions_sub), settings.suggestions) {
                        scope.launch { SettingsStore.setSuggestions(context, it) }
                    }
                },
                {
                    SwitchTile(KeyIcons.Done, stringResource(R.string.settings_autocorrect), stringResource(R.string.settings_autocorrect_sub), settings.autocorrect) {
                        scope.launch { SettingsStore.setAutocorrect(context, it) }
                    }
                },
            ),
        )
        SectionHeader(stringResource(R.string.settings_typing_caps_spacing))
        SegmentedGroup(
            listOf<@Composable () -> Unit>(
                {
                    SwitchTile(KeyIcons.ShiftOn, stringResource(R.string.settings_auto_caps), stringResource(R.string.settings_auto_caps_sub), settings.autoCaps) {
                        scope.launch { SettingsStore.setAutoCaps(context, it) }
                    }
                },
                {
                    SwitchTile(KeyIcons.Next, stringResource(R.string.settings_auto_space), stringResource(R.string.settings_auto_space_sub), settings.autoSpace) {
                        scope.launch { SettingsStore.setAutoSpace(context, it) }
                    }
                },
            ),
        )
    }
}

/** Gestures on the keys, and the space bar. */
@Composable
private fun KeysScreen(settings: ZenboardSettings, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    CategoryPage(stringResource(R.string.settings_section_keys), onBack) {
        SectionHeader(stringResource(R.string.settings_gestures_keys))
        SegmentedGroup(
            listOf<@Composable () -> Unit>(
                {
                    SwitchTile(KeyIcons.Words, stringResource(R.string.settings_glide), stringResource(R.string.settings_glide_sub), settings.glide) {
                        scope.launch { SettingsStore.setGlide(context, it) }
                    }
                },
                {
                    SwitchTile(KeyIcons.Backspace, stringResource(R.string.settings_backspace_slide), stringResource(R.string.settings_backspace_slide_sub), settings.backspaceSlide) {
                        scope.launch { SettingsStore.setBackspaceSlide(context, it) }
                    }
                },
                {
                    SwitchTile(
                        KeyIcons.Backspace,
                        stringResource(R.string.settings_backspace_double_tap),
                        stringResource(R.string.settings_backspace_double_tap_sub),
                        settings.backspaceDoubleTap,
                    ) { scope.launch { SettingsStore.setBackspaceDoubleTap(context, it) } }
                },
            ),
        )
        SectionHeader(stringResource(R.string.settings_section_space_bar))
        SegmentedGroup(
            listOf<@Composable () -> Unit>(
                {
                    SwitchTile(KeyIcons.Next, stringResource(R.string.settings_space_slide), stringResource(R.string.settings_space_slide_sub), settings.spaceSlide) {
                        scope.launch { SettingsStore.setSpaceSlide(context, it) }
                    }
                },
                {
                    SwitchTile(KeyIcons.Globe, stringResource(R.string.settings_space_switch), stringResource(R.string.settings_space_switch_sub), settings.spaceSwitch) {
                        scope.launch { SettingsStore.setSpaceSwitch(context, it) }
                    }
                },
                {
                    SwitchTile(KeyIcons.Words, stringResource(R.string.settings_space_label), stringResource(R.string.settings_space_label_sub), settings.spaceLabel) {
                        scope.launch { SettingsStore.setSpaceLabel(context, it) }
                    }
                },
            ),
        )
    }
}

/** Theme, key size and popup, sound and vibration. */
@Composable
private fun LookScreen(settings: ZenboardSettings, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    CategoryPage(stringResource(R.string.settings_section_look), onBack) {
        SectionHeader(stringResource(R.string.settings_appearance_theme))
        SegmentedGroup(
            listOf<@Composable () -> Unit>(
                {
                    SwitchTile(KeyIcons.DarkMode, stringResource(R.string.settings_pure_black), stringResource(R.string.settings_pure_black_sub), settings.pureBlack) {
                        scope.launch { SettingsStore.setPureBlack(context, it) }
                    }
                },
            ),
        )
        SectionHeader(stringResource(R.string.settings_typing_keys))
        SegmentedGroup(
            listOf<@Composable () -> Unit>(
                {
                    SliderTile(
                        icon = KeyIcons.Height,
                        title = stringResource(R.string.settings_key_height),
                        value = settings.keyHeight,
                        range = KEY_HEIGHT_MIN..KEY_HEIGHT_MAX,
                        steps = 9, // 5% steps from 80% to 130%
                        valueLabel = { "${(it * 100).roundToInt()}%" },
                        onValueChangeFinished = { scope.launch { SettingsStore.setKeyHeight(context, it) } },
                    )
                },
                {
                    SwitchTile(KeyIcons.Bubble, stringResource(R.string.settings_key_popup), stringResource(R.string.settings_key_popup_sub), settings.keyPopup) {
                        scope.launch { SettingsStore.setKeyPopup(context, it) }
                    }
                },
            ),
        )
        SectionHeader(stringResource(R.string.settings_section_feedback))
        SegmentedGroup(
            listOf<@Composable () -> Unit>(
                {
                    SwitchTile(KeyIcons.Vibration, stringResource(R.string.settings_haptics), stringResource(R.string.settings_haptics_sub), settings.haptics) {
                        scope.launch { SettingsStore.setHaptics(context, it) }
                    }
                },
                {
                    SwitchTile(KeyIcons.Sound, stringResource(R.string.settings_sound), stringResource(R.string.settings_sound_sub), settings.sound) {
                        scope.launch { SettingsStore.setSound(context, it) }
                    }
                },
            ),
        )
    }
}

private fun retentionLabel(r: ClipRetention) = when (r) {
    ClipRetention.HOUR -> R.string.settings_clip_hour
    ClipRetention.DAY -> R.string.settings_clip_day
    ClipRetention.FOREVER -> R.string.settings_clip_forever
}

@Composable
private fun ClipboardScreen(settings: ZenboardSettings, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    CategoryPage(stringResource(R.string.settings_section_clipboard), onBack) {
        SectionHeader(stringResource(R.string.settings_clip_history))
        SegmentedGroup(
            listOf<@Composable () -> Unit>(
                {
                    SwitchTile(
                        icon = KeyIcons.Clipboard,
                        title = stringResource(R.string.settings_clip_history),
                        subtitle = stringResource(R.string.settings_clip_history_sub),
                        checked = settings.clipHistory,
                        onCheckedChange = { scope.launch { SettingsStore.setClipHistory(context, it) } },
                    )
                },
            ),
        )
        // Counts read from disk when the page opens and after each clear.
        var saved by remember { mutableStateOf<List<Clip>?>(null) }
        var confirm by remember { mutableStateOf<Boolean?>(null) } // true: clear pinned too
        val store = remember { ClipStore(context) }
        LaunchedEffect(Unit) { saved = store.load() }
        val unpinned = saved?.count { !it.pinned } ?: 0
        val pinned = saved?.count { it.pinned } ?: 0
        SectionHeader(stringResource(R.string.settings_clip_saved))
        SegmentedGroup(
            listOf<@Composable () -> Unit>(
                {
                    GroupTile(
                        badge = { ScallopBadge(KeyIcons.Close, ZenithColors.indicator, ZenithColors.onIndicator) },
                        title = stringResource(R.string.settings_clip_clear),
                        subtitle = pluralStringResource(R.plurals.settings_clip_unpinned, unpinned, unpinned),
                        onClick = if (unpinned > 0) { { confirm = false } } else null,
                    )
                },
                {
                    GroupTile(
                        badge = { ScallopBadge(KeyIcons.Pin, ZenithColors.indicator, ZenithColors.onIndicator) },
                        title = stringResource(R.string.settings_clip_clear_all),
                        subtitle = pluralStringResource(R.plurals.settings_clip_pinned, pinned, pinned),
                        onClick = if (unpinned + pinned > 0) { { confirm = true } } else null,
                    )
                },
            ),
        )
        confirm?.let { all ->
            AlertDialog(
                onDismissRequest = { confirm = null },
                title = { Text(stringResource(if (all) R.string.settings_clip_clear_all else R.string.settings_clip_clear)) },
                text = { Text(stringResource(if (all) R.string.settings_clip_clear_all_confirm else R.string.settings_clip_clear_confirm)) },
                confirmButton = {
                    TextButton(onClick = {
                        confirm = null
                        scope.launch {
                            store.clear(includePinned = all)
                            saved = store.load()
                        }
                    }) { Text(stringResource(R.string.settings_clip_clear_button)) }
                },
                dismissButton = { TextButton(onClick = { confirm = null }) { Text(stringResource(android.R.string.cancel)) } },
            )
        }
        if (settings.clipHistory) {
            SectionHeader(stringResource(R.string.settings_clip_keep))
            SegmentedGroup(
                ClipRetention.entries.map { r ->
                    @Composable {
                        GroupTile(
                            badge = null,
                            title = stringResource(retentionLabel(r)),
                            subtitle = if (r == ClipRetention.FOREVER) stringResource(R.string.settings_clip_forever_sub) else "",
                            modifier = Modifier.selectable(
                                selected = settings.clipRetention == r,
                                role = Role.RadioButton,
                                onClick = { scope.launch { SettingsStore.setClipRetention(context, r) } },
                            ),
                            trailing = { RadioButton(selected = settings.clipRetention == r, onClick = null) },
                        )
                    }
                },
            )
        }
    }
}
