package app.zenith.zenboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Category tabs (recents first) over a scrolling grid. Same height as the letter rows it replaces. */
@Composable
fun EmojiPanel(categories: List<EmojiCategory>?, recents: List<String>, height: Dp, onEmoji: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val feedback = LocalKeyFeedback.current
    // Tab 0 is recents; open on the first category until something has been used.
    var tab by remember { mutableIntStateOf(if (recents.isEmpty()) 1 else 0) }
    val recentsLabel = stringResource(R.string.emoji_recents)
    Column(Modifier.fillMaxWidth().height(height)) {
        if (categories == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.emoji_loading), color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            return@Column
        }
        Row(Modifier.fillMaxWidth().height(40.dp)) {
            val tabs = listOf("🕘" to recentsLabel) + categories.map { it.icon to it.name }
            tabs.forEachIndexed { i, (icon, name) ->
                val selected = i == tab
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) ZenithColors.indicator else Color.Transparent)
                        .clickable(role = Role.Tab) { tab = i }
                        .semantics {
                            contentDescription = name
                            this.selected = selected
                        },
                    contentAlignment = Alignment.Center,
                ) { Text(icon, fontSize = 18.sp) }
            }
        }
        val items = if (tab == 0) recents else categories[tab - 1].emoji
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.emoji_recents_empty), color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            // key(tab): each category starts at its top.
            key(tab) {
                LazyVerticalGrid(GridCells.Adaptive(44.dp), Modifier.weight(1f).fillMaxWidth()) {
                    items(items) { emoji ->
                        Box(
                            Modifier
                                .aspectRatio(1f)
                                .padding(1.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    feedback.press()
                                    onEmoji(emoji)
                                },
                            contentAlignment = Alignment.Center,
                        ) { Text(emoji, fontSize = 26.sp) }
                    }
                }
            }
        }
    }
}
