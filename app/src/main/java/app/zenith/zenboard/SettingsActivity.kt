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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private enum class Page { HOME, SETUP, ABOUT }

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
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        val forward = targetState != Page.HOME
                        (fadeIn(Motion.effects(220, delay = 60)) +
                            slideInHorizontally(Motion.spatial(380)) { w -> if (forward) w / 6 else -w / 6 })
                            .togetherWith(fadeOut(Motion.effects(90)))
                    },
                    label = "page",
                ) { target ->
                    when (target) {
                        Page.HOME -> HomeScreen(
                            settings = current,
                            enabled = enabled,
                            selected = selected,
                            versionName = versionName,
                            onOpenSetup = { page = Page.SETUP },
                            onOpenAbout = { page = Page.ABOUT },
                        )
                        Page.SETUP -> SetupScreen(
                            enabled = enabled,
                            selected = selected,
                            onEnable = { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                            onSelect = { getSystemService(InputMethodManager::class.java).showInputMethodPicker() },
                            onBack = { page = Page.HOME },
                        )
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

@Composable
private fun HomeScreen(
    settings: ZenboardSettings,
    enabled: Boolean,
    selected: Boolean,
    versionName: String,
    onOpenSetup: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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

        val ready = enabled && selected
        SectionHeader(stringResource(R.string.settings_section_setup))
        SegmentedGroup(
            listOf {
                GroupTile(
                    badge = {
                        if (ready) {
                            ScallopBadge(KeyIcons.Done, cs.secondary, cs.onSecondary)
                        } else {
                            ScallopBadge(KeyIcons.Keyboard, cs.errorContainer, cs.onErrorContainer)
                        }
                    },
                    title = stringResource(R.string.settings_setup_title),
                    subtitle = stringResource(
                        when {
                            ready -> R.string.settings_setup_ready
                            !enabled -> R.string.settings_setup_not_enabled
                            else -> R.string.settings_setup_not_selected
                        },
                    ),
                    subtitleColor = if (ready) cs.onSurfaceVariant else cs.error,
                    onClick = onOpenSetup,
                )
            },
        )

        SectionHeader(stringResource(R.string.settings_section_appearance))
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

        SectionHeader(stringResource(R.string.settings_section_typing))
        SegmentedGroup(
            listOf {
                SwitchTile(
                    icon = KeyIcons.Bubble,
                    title = stringResource(R.string.settings_key_popup),
                    subtitle = stringResource(R.string.settings_key_popup_sub),
                    checked = settings.keyPopup,
                    onCheckedChange = { scope.launch { SettingsStore.setKeyPopup(context, it) } },
                )
            },
        )

        SectionHeader(stringResource(R.string.settings_section_about))
        SegmentedGroup(
            listOf {
                GroupTile(
                    badge = { ScallopBadge(KeyIcons.Info, ZenithColors.indicator, ZenithColors.onIndicator) },
                    title = stringResource(R.string.settings_about_title),
                    subtitle = stringResource(R.string.about_version, versionName),
                    onClick = onOpenAbout,
                )
            },
        )

        SectionHeader(stringResource(R.string.setup_try_section))
        TryField()
        Spacer(Modifier.height(24.dp))
    }
}
