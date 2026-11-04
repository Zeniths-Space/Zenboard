package app.zenith.zenboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** App identity, privacy summary, license status and third-party credits. */
@Composable
fun AboutScreen(versionName: String, onBack: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    CategoryPage(stringResource(R.string.about_title), onBack, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(16.dp))
        // The launcher icon's own layers, so the page always matches the icon.
        Box(Modifier.size(96.dp).clip(RoundedCornerShape(28.dp))) {
            Image(painterResource(R.drawable.ic_launcher_background), contentDescription = null, modifier = Modifier.fillMaxSize())
            Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.about_version, versionName),
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.about_description),
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Column(Modifier.fillMaxWidth()) {
            SectionHeader(stringResource(R.string.about_section_app))
            SegmentedGroup(
                listOf(
                    {
                        GroupTile(
                            badge = { ScallopBadge(KeyIcons.Keyboard, ZenithColors.indicator, ZenithColors.onIndicator) },
                            title = stringResource(R.string.about_developer),
                            subtitle = stringResource(R.string.about_developer_name),
                        )
                    },
                    {
                        GroupTile(
                            badge = { ScallopBadge(KeyIcons.Lock, cs.tertiaryContainer, cs.onTertiaryContainer) },
                            title = stringResource(R.string.setup_privacy_title),
                            subtitle = stringResource(R.string.setup_privacy_sub),
                        )
                    },
                    {
                        GroupTile(
                            badge = { ScallopBadge(KeyIcons.Info, ZenithColors.indicator, ZenithColors.onIndicator) },
                            title = stringResource(R.string.about_license),
                            subtitle = stringResource(R.string.about_license_sub),
                        )
                    },
                ),
            )
            SectionHeader(stringResource(R.string.about_section_credits))
            SegmentedGroup(
                listOf(
                    {
                        GroupTile(
                            badge = { ScallopBadge(KeyIcons.Emoji, ZenithColors.indicator, ZenithColors.onIndicator) },
                            title = stringResource(R.string.about_emoji_data),
                            subtitle = stringResource(R.string.about_emoji_data_sub),
                        )
                    },
                    {
                        GroupTile(
                            badge = { ScallopBadge(KeyIcons.Words, ZenithColors.indicator, ZenithColors.onIndicator) },
                            title = stringResource(R.string.about_word_list),
                            subtitle = stringResource(R.string.about_word_list_sub),
                        )
                    },
                    {
                        GroupTile(
                            badge = { ScallopBadge(KeyIcons.Done, ZenithColors.indicator, ZenithColors.onIndicator) },
                            title = stringResource(R.string.about_icons),
                            subtitle = stringResource(R.string.about_icons_sub),
                        )
                    },
                ),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
