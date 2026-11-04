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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class Page { HOME, SETUP, APPEARANCE, TYPING, SPACE_BAR, GESTURES, CLIPBOARD, FEEDBACK, ABOUT }

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
                        Page.APPEARANCE -> WithTryKeyboard { AppearanceScreen(current, onBack = { page = Page.HOME }) }
                        Page.TYPING -> WithTryKeyboard { TypingScreen(current, onBack = { page = Page.HOME }) }
                        Page.SPACE_BAR -> WithTryKeyboard { SpaceBarScreen(current, onBack = { page = Page.HOME }) }
                        Page.GESTURES -> WithTryKeyboard { GesturesScreen(current, onBack = { page = Page.HOME }) }
                        Page.CLIPBOARD -> WithTryKeyboard { ClipboardScreen(current, onBack = { page = Page.HOME }) }
                        Page.FEEDBACK -> WithTryKeyboard { FeedbackScreen(current, onBack = { page = Page.HOME }) }
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

/** Categories only; each opens its own page. */
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
    Column(
        Modifier
            .fillMaxSize()
            .background(cs.surface)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScallopBadge(KeyIcons.Keyboard, cs.secondary, cs.onSecondary, size = 64.dp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.setup_tagline), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(28.dp))
        SegmentedGroup(
            listOf(
                {
                    GroupTile(
                        badge = {
                            if (ready) {
                                ScallopBadge(KeyIcons.Done, ZenithColors.indicator, ZenithColors.onIndicator)
                            } else {
                                ScallopBadge(KeyIcons.Keyboard, cs.errorContainer, cs.onErrorContainer)
                            }
                        },
                        title = stringResource(R.string.settings_section_setup),
                        subtitle = stringResource(
                            when {
                                ready -> R.string.settings_setup_ready
                                !enabled -> R.string.settings_setup_not_enabled
                                else -> R.string.settings_setup_not_selected
                            },
                        ),
                        subtitleColor = if (ready) cs.onSurfaceVariant else cs.error,
                        onClick = { onOpen(Page.SETUP) },
                    )
                },
                {
                    GroupTile(
                        badge = { ScallopBadge(KeyIcons.DarkMode, ZenithColors.indicator, ZenithColors.onIndicator) },
                        title = stringResource(R.string.settings_section_appearance),
                        subtitle = stringResource(
                            if (settings.pureBlack) R.string.settings_appearance_summary_black else R.string.settings_appearance_summary,
                        ),
                        onClick = { onOpen(Page.APPEARANCE) },
                    )
                },
                {
                    GroupTile(
                        badge = { ScallopBadge(KeyIcons.Words, ZenithColors.indicator, ZenithColors.onIndicator) },
                        title = stringResource(R.string.settings_section_typing),
                        subtitle = stringResource(
                            R.string.settings_typing_summary,
                            onOff(settings.suggestions),
                            onOff(settings.autocorrect),
                        ),
                        onClick = { onOpen(Page.TYPING) },
                    )
                },
                {
                    GroupTile(
                        badge = { ScallopBadge(KeyIcons.Keyboard, ZenithColors.indicator, ZenithColors.onIndicator) },
                        title = stringResource(R.string.settings_section_space_bar),
                        subtitle = stringResource(
                            R.string.settings_space_bar_summary,
                            onOff(settings.spaceSlide),
                            onOff(settings.spaceSwitch),
                            onOff(settings.spaceLabel),
                        ),
                        onClick = { onOpen(Page.SPACE_BAR) },
                    )
                },
                {
                    GroupTile(
                        badge = { ScallopBadge(KeyIcons.Backspace, ZenithColors.indicator, ZenithColors.onIndicator) },
                        title = stringResource(R.string.settings_section_gestures),
                        subtitle = stringResource(R.string.settings_gestures_summary, onOff(settings.glide), onOff(settings.backspaceSlide), onOff(settings.backspaceDoubleTap)),
                        onClick = { onOpen(Page.GESTURES) },
                    )
                },
                {
                    GroupTile(
                        badge = { ScallopBadge(KeyIcons.History, ZenithColors.indicator, ZenithColors.onIndicator) },
                        title = stringResource(R.string.settings_section_clipboard),
                        subtitle = if (settings.clipHistory) {
                            stringResource(R.string.settings_clipboard_summary_on, stringResource(retentionLabel(settings.clipRetention)).lowercase())
                        } else {
                            stringResource(R.string.settings_clipboard_summary_off)
                        },
                        onClick = { onOpen(Page.CLIPBOARD) },
                    )
                },
                {
                    GroupTile(
                        badge = { ScallopBadge(KeyIcons.Vibration, ZenithColors.indicator, ZenithColors.onIndicator) },
                        title = stringResource(R.string.settings_section_feedback),
                        subtitle = stringResource(
                            R.string.settings_feedback_summary,
                            onOff(settings.haptics),
                            onOff(settings.sound),
                        ),
                        onClick = { onOpen(Page.FEEDBACK) },
                    )
                },
                {
                    GroupTile(
                        badge = { ScallopBadge(KeyIcons.Info, ZenithColors.indicator, ZenithColors.onIndicator) },
                        title = stringResource(R.string.settings_section_about),
                        subtitle = stringResource(R.string.about_version, versionName),
                        onClick = { onOpen(Page.ABOUT) },
                    )
                },
            ),
        )
        Spacer(Modifier.height(FabClearance))
    }
}

@Composable
private fun AppearanceScreen(settings: ZenboardSettings, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    CategoryPage(stringResource(R.string.settings_section_appearance), onBack) {
        SectionHeader(stringResource(R.string.settings_appearance_theme))
        SegmentedGroup(
            listOf {
                SwitchTile(
                    icon = KeyIcons.DarkMode,
                    title = stringResource(R.string.settings_pure_black),
                    subtitle = stringResource(R.string.settings_pure_black_sub),
                    checked = settings.pureBlack,
                    onCheckedChange = { scope.launch { SettingsStore.setPureBlack(context, it) } },
                )
            },
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
                    SwitchTile(
                        icon = KeyIcons.Bubble,
                        title = stringResource(R.string.settings_key_popup),
                        subtitle = stringResource(R.string.settings_key_popup_sub),
                        checked = settings.keyPopup,
                        onCheckedChange = { scope.launch { SettingsStore.setKeyPopup(context, it) } },
                    )
                },
            ),
        )
    }
}

@Composable
private fun FeedbackScreen(settings: ZenboardSettings, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    CategoryPage(stringResource(R.string.settings_section_feedback), onBack) {
        Spacer(Modifier.height(8.dp))
        SegmentedGroup(
            listOf<@Composable () -> Unit>(
                {
                    SwitchTile(
                        icon = KeyIcons.Vibration,
                        title = stringResource(R.string.settings_haptics),
                        subtitle = stringResource(R.string.settings_haptics_sub),
                        checked = settings.haptics,
                        onCheckedChange = { scope.launch { SettingsStore.setHaptics(context, it) } },
                    )
                },
                {
                    SwitchTile(
                        icon = KeyIcons.Sound,
                        title = stringResource(R.string.settings_sound),
                        subtitle = stringResource(R.string.settings_sound_sub),
                        checked = settings.sound,
                        onCheckedChange = { scope.launch { SettingsStore.setSound(context, it) } },
                    )
                },
            ),
        )
    }
}

@Composable
private fun TypingScreen(settings: ZenboardSettings, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    CategoryPage(stringResource(R.string.settings_section_typing), onBack) {
        SectionHeader(stringResource(R.string.settings_typing_words))
        SegmentedGroup(
            listOf<@Composable () -> Unit>(
                {
                    SwitchTile(
                        icon = KeyIcons.Words,
                        title = stringResource(R.string.settings_suggestions),
                        subtitle = stringResource(R.string.settings_suggestions_sub),
                        checked = settings.suggestions,
                        onCheckedChange = { scope.launch { SettingsStore.setSuggestions(context, it) } },
                    )
                },
                {
                    SwitchTile(
                        icon = KeyIcons.Done,
                        title = stringResource(R.string.settings_autocorrect),
                        subtitle = stringResource(R.string.settings_autocorrect_sub),
                        checked = settings.autocorrect,
                        onCheckedChange = { scope.launch { SettingsStore.setAutocorrect(context, it) } },
                    )
                },
            ),
        )
    }
}

@Composable
private fun SpaceBarScreen(settings: ZenboardSettings, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    CategoryPage(stringResource(R.string.settings_section_space_bar), onBack) {
        SegmentedGroup(
            listOf<@Composable () -> Unit>(
                {
                    SwitchTile(
                        icon = KeyIcons.Next,
                        title = stringResource(R.string.settings_space_slide),
                        subtitle = stringResource(R.string.settings_space_slide_sub),
                        checked = settings.spaceSlide,
                        onCheckedChange = { scope.launch { SettingsStore.setSpaceSlide(context, it) } },
                    )
                },
                {
                    SwitchTile(
                        icon = KeyIcons.Globe,
                        title = stringResource(R.string.settings_space_switch),
                        subtitle = stringResource(R.string.settings_space_switch_sub),
                        checked = settings.spaceSwitch,
                        onCheckedChange = { scope.launch { SettingsStore.setSpaceSwitch(context, it) } },
                    )
                },
                {
                    SwitchTile(
                        icon = KeyIcons.Words,
                        title = stringResource(R.string.settings_space_label),
                        subtitle = stringResource(R.string.settings_space_label_sub),
                        checked = settings.spaceLabel,
                        onCheckedChange = { scope.launch { SettingsStore.setSpaceLabel(context, it) } },
                    )
                },
            ),
        )
    }
}

@Composable
private fun GesturesScreen(settings: ZenboardSettings, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    CategoryPage(stringResource(R.string.settings_section_gestures), onBack) {
        SegmentedGroup(
            listOf<@Composable () -> Unit>(
                {
                    SwitchTile(
                        icon = KeyIcons.Words,
                        title = stringResource(R.string.settings_glide),
                        subtitle = stringResource(R.string.settings_glide_sub),
                        checked = settings.glide,
                        onCheckedChange = { scope.launch { SettingsStore.setGlide(context, it) } },
                    )
                },
                {
                    SwitchTile(
                        icon = KeyIcons.Backspace,
                        title = stringResource(R.string.settings_backspace_slide),
                        subtitle = stringResource(R.string.settings_backspace_slide_sub),
                        checked = settings.backspaceSlide,
                        onCheckedChange = { scope.launch { SettingsStore.setBackspaceSlide(context, it) } },
                    )
                },
                {
                    SwitchTile(
                        icon = KeyIcons.Backspace,
                        title = stringResource(R.string.settings_backspace_double_tap),
                        subtitle = stringResource(R.string.settings_backspace_double_tap_sub),
                        checked = settings.backspaceDoubleTap,
                        onCheckedChange = { scope.launch { SettingsStore.setBackspaceDoubleTap(context, it) } },
                    )
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
        SegmentedGroup(
            listOf<@Composable () -> Unit>(
                {
                    SwitchTile(
                        icon = KeyIcons.History,
                        title = stringResource(R.string.settings_clip_history),
                        subtitle = stringResource(R.string.settings_clip_history_sub),
                        checked = settings.clipHistory,
                        onCheckedChange = { scope.launch { SettingsStore.setClipHistory(context, it) } },
                    )
                },
            ),
        )
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

@Composable
private fun onOff(on: Boolean): String = stringResource(if (on) R.string.settings_state_on else R.string.settings_state_off)
