package com.elara.keyboard

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Search, then category tabs (recents first), over a scrolling grid. Same height as the letter rows it replaces. */
@Composable
fun EmojiPanel(
    categories: List<EmojiCategory>?,
    recents: List<String>,
    tones: EmojiTones,
    /** 0 is recents, then the categories in order. */
    tab: Int,
    onTab: (Int) -> Unit,
    height: Dp,
    onSearch: () -> Unit,
    onRemoveRecent: (String) -> Unit,
    onEmoji: (String) -> Unit,
    onTextExpression: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val feedback = LocalKeyFeedback.current
    val recentsLabel = stringResource(R.string.emoji_recents)
    val kaomojiLabel = stringResource(R.string.emoji_kaomoji)
    val pairsLabel = stringResource(R.string.emoji_pairs)
    Column(Modifier.fillMaxWidth().height(height)) {
        if (categories == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.emoji_loading), color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            return@Column
        }
        Row(Modifier.fillMaxWidth().height(40.dp)) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button) {
                        feedback.press()
                        onSearch()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(KeyIcons.Search, stringResource(R.string.emoji_search), tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            val kaomojiTab = categories.size + 1
            val pairsTab = categories.size + 2
            val tabs = listOf("🕘" to recentsLabel) + categories.map { it.icon to it.name } +
                listOf("ヅ" to kaomojiLabel, "💞" to pairsLabel)
            tabs.forEachIndexed { i, (icon, name) ->
                val selected = i == tab
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) ZenithColors.indicator else Color.Transparent)
                        .clickable(role = Role.Tab) { onTab(i) }
                        .semantics {
                            contentDescription = name
                            this.selected = selected
                        },
                    contentAlignment = Alignment.Center,
                ) { Text(icon, fontSize = 18.sp) }
            }
        }
        val kaomojiTab = categories.size + 1
        val pairsTab = categories.size + 2
        val kaomoji = tab == kaomojiTab
        val pairs = tab == pairsTab
        val items = when {
            tab == 0 -> recents
            kaomoji -> KAOMOJI
            pairs -> EMOJI_PAIRS
            else -> categories[tab - 1].emoji
        }
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.emoji_recents_empty), color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            // key(tab): each category starts at its top.
            key(tab) {
                val text = kaomoji || pairs
                // Own column width per tab, so the columns share the full width with no leftover gap.
                val cells = GridCells.Adaptive(if (kaomoji) 112.dp else if (pairs) 76.dp else 44.dp)
                LazyVerticalGrid(cells, Modifier.weight(1f).fillMaxWidth()) {
                    items(items) { emoji ->
                        // Recents: long-press also offers removing that one emoji.
                        EmojiCell(
                            emoji, tones, Modifier.height(44.dp),
                            onEmoji = if (text) onTextExpression else onEmoji,
                            fontSize = if (kaomoji) 15.sp else if (pairs) 22.sp else 26.sp,
                            onRemove = if (tab == 0) { { onRemoveRecent(emoji) } } else null,
                        )
                    }
                }
            }
        }
    }
}

/** "Clear all", between ABC and delete on the recents tab. Two taps, so one stray touch does not wipe them. */
@Composable
fun ClearRecents(onClear: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val feedback = LocalKeyFeedback.current
    var armed by remember { mutableStateOf(false) }
    Text(
        stringResource(if (armed) R.string.emoji_clear_recents_confirm else R.string.emoji_clear_recents),
        color = if (armed) cs.error else cs.primary,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button) {
                feedback.press()
                if (armed) onClear() else armed = true
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/**
 * Emoji search: the query (typed on the letter keys below) and one row of results. An empty query
 * shows recents. Close returns to the emoji panel.
 */
@Composable
fun EmojiSearchBar(
    query: String,
    index: List<EmojiInfo>?,
    recents: List<String>,
    tones: EmojiTones,
    onClose: () -> Unit,
    onEmoji: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val feedback = LocalKeyFeedback.current
    val results = remember(query, index) { if (index == null) emptyList() else searchEmoji(index, query) }
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(cs.surfaceContainerHigh),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(role = Role.Button) {
                        feedback.press()
                        onClose()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(KeyIcons.Previous, stringResource(R.string.emoji_search_close), tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Text(
                query.ifEmpty { stringResource(R.string.emoji_search) },
                color = if (query.isEmpty()) cs.onSurfaceVariant else cs.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.StartEllipsis,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
        }
        val items = if (query.isBlank()) recents else results
        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
            when {
                index == null -> Text(stringResource(R.string.emoji_loading), color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                items.isEmpty() && query.isNotBlank() -> Text(stringResource(R.string.emoji_no_results), color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                else -> LazyRow(Modifier.fillMaxSize()) {
                    items(items) { emoji ->
                        EmojiCell(emoji, tones, Modifier.fillMaxHeight().width(48.dp), onEmoji)
                    }
                }
            }
        }
    }
}

/**
 * One emoji, shown in its picked skin tone. Tap types it; long-press opens a row above it with its
 * tones and, given [onRemove], a remove button. Screen readers get each as an action.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EmojiCell(
    emoji: String,
    tones: EmojiTones,
    modifier: Modifier,
    onEmoji: (String) -> Unit,
    fontSize: androidx.compose.ui.unit.TextUnit = 26.sp,
    onRemove: (() -> Unit)? = null,
) {
    val feedback = LocalKeyFeedback.current
    val shown = tones.shown(emoji)
    val options = tones.options(shown)
    var picking by remember { mutableStateOf(false) }
    val toneLabels = TONE_LABELS.map { stringResource(it) }
    val removeLabel = stringResource(R.string.emoji_remove_recent)
    Box(
        modifier
            .padding(1.dp)
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = {
                    feedback.press()
                    onEmoji(shown)
                },
                onLongClick = if (options.isEmpty() && onRemove == null) null else {
                    {
                        feedback.haptic(HapticFeedbackConstants.LONG_PRESS)
                        picking = true
                    }
                },
            )
            .semantics {
                val actions = options.mapIndexed { i, option ->
                    CustomAccessibilityAction(toneLabels[i]) { onEmoji(option); true }
                } + listOfNotNull(onRemove?.let { remove -> CustomAccessibilityAction(removeLabel) { remove(); true } })
                if (actions.isNotEmpty()) customActions = actions
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(shown, fontSize = fontSize, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
        if (picking) {
            Popup(
                popupPositionProvider = AboveAnchor,
                onDismissRequest = { picking = false },
                // Not clipped: the row may rise above the keyboard window.
                properties = PopupProperties(clippingEnabled = false),
            ) {
                Row(
                    Modifier
                        .shadow(6.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(4.dp),
                ) {
                    options.forEachIndexed { i, option ->
                        Box(
                            Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (option == shown) ZenithColors.indicator else Color.Transparent)
                                .clickable {
                                    feedback.press()
                                    picking = false
                                    onEmoji(option)
                                }
                                .semantics { contentDescription = toneLabels[i] },
                            contentAlignment = Alignment.Center,
                        ) { Text(option, fontSize = 26.sp) }
                    }
                    if (onRemove != null) {
                        Box(
                            Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(role = Role.Button) {
                                    feedback.press()
                                    picking = false
                                    onRemove()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(KeyIcons.Close, removeLabel, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}

private val KAOMOJI = listOf(
    // happy
    "(＾▽＾)", "(￣▽￣)", "ヽ(・∀・)ﾉ", "٩(◕‿◕｡)۶", "(✿◠‿◠)", "(•̀ᴗ•́)و", "(≧◡≦)", "(｡•̀ᴗ-)✧",
    "(*^‿^*)", "o(≧▽≦)o", "\\(^o^)/", "(๑˃ᴗ˂)ﻭ", "(｡◕‿◕｡)", "ヾ(＾∇＾)", "(´▽`ʃ♡ƪ)", "(⌒‿⌒)",
    "(ノ^_^)ノ", "＼(＾O＾)／", "(*≧ω≦)",
    // love
    "(｡♥‿♥｡)", "(♡˙︶˙♡)", "(づ｡◕‿‿◕｡)づ", "(づ￣ ³￣)づ", "(◍•ᴗ•◍)❤", "(ˆ⌣ˆ)♡", "( ˘ ³˘)♥", "(´,,•ω•,,)♡",
    "(♥ω♥*)", "♡(◔◡◔)", "(*¯︶¯*)", "(⁄ ⁄>⁄ ▽ ⁄<⁄ ⁄)", "(〃▽〃)", "(/▽＼)", "(´∀`)♡", "(◕‿◕)♡",
    // sad
    "(╥﹏╥)", "(ಥ﹏ಥ)", "(T_T)", "(；へ：)", "(；︵；)", "(ノД`)", "(｡•́︿•̀｡)", "(つ﹏⊂)",
    "(இ﹏இ`｡)", "(´;ω;`)", "( ´•̥̥̥ω•̥̥̥` )", "(ㄒoㄒ)", "(｡ŏ﹏ŏ)", "(◞‸◟)",
    // angry
    "ლ(ಠ益ಠლ)", "(ノಠ益ಠ)ノ彡┻━┻", "(╬ಠ益ಠ)", "(｀Д´)", "(ㆆ_ㆆ)", "(¬_¬)", "(눈_눈)", "(҂◡_◡)",
    "٩(╬ʘ益ʘ╬)۶", "(⋟﹏⋞)", "(ง'̀-'́)ง", "(｀皿´＃)", "ヽ(`Д´)ﾉ", "ಠ_ಠ", "(`ε´)",
    // shrug / meh / confused
    "¯\\_(ツ)_/¯", "(¬‿¬)", "(・_・;)", "(°ー°〃)", "(・・ ) ?", "(´･_･`)", "(￢_￢)", "(－‸ლ)",
    "(°△°|||)", "(⊙_⊙)", "(◎_◎;)", "(⊙ˍ⊙)", "(;¬_¬)", "(._.)", "(・–・)",
    // surprised / shy / embarrassed
    "Σ(°ロ°)", "(☉_☉)", "(ﾟДﾟ;)", "w(ﾟДﾟ)w", "(*ﾟーﾟ)", "(⁄⁄•⁄ω⁄•⁄⁄)", "(〃＞＿＜;〃)", "(>_<)",
    "(⁄ ⁄•⁄ω⁄•⁄ ⁄)", "(///▽///)", "(๑•́ ₃ •̀๑)", "(*/ω＼)",
    // cool / smug / wink
    "(⌐■_■)", "( ͡° ͜ʖ ͡°)", "(｀∀´)Ψ", "(¬‿¬ )", "(◕‿↼)", "(^_~)", "(＾ｖ＾)",
    "(☞ﾟヮﾟ)☞", "☜(ﾟヮﾟ☜)", "(ᵔᴥᵔ)", "(◔‿◔)", "(≖‿≖)", "(｡•̀ᴗ-)b", "d(^_^)b", "(^_−)☆",
    // celebrate / magic
    "(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧", "✧٩(ˊᗜˋ*)و✧", "(ﾉ´ヮ`)ﾉ*: ･ﾟ", "(∩^o^)⊃━☆ﾟ.*", "(҂‾ ▵‾)︻デ═一", "ヽ(´▽`)/", "(ﾉ≧∀≦)ﾉ",
    // tired / sleep / sick
    "(－_－) zzz", "(´-ω-`)", "( ˘ω˘ )ｽﾔｧ", "(✖╭╮✖)", "(×_×)", "(@_@)", "(*_*)", "(︶｡︶)",
    "(x_x)", "(~_~;)", "(´Д`)", "(ー_ー)!!", "(_ _)zzZ",
    // animals
    "ʕ•ᴥ•ʔ", "ʕ´•ᴥ•`ʔ", "(=^･ω･^=)", "(=^･ｪ･^=)", "(ΦωΦ)", "(＾• ω •＾)", "ฅ^•ﻌ•^ฅ", "U・ᴥ・U",
    "(◕ᴥ◕ʋ)", "⊂(・(ェ)・)⊃", "(´(ｪ)`)", "(ᵔᴥᵔ)", "૮ ˶ᵔ ᵕ ᵔ˶ ა", "(=①ω①=)", "(◕ᴥ◕)", "ʕ·͡ᴥ·ʔ",
    "(°◡°♡)", "~(˘▾˘~)", "(⁎˃ᆺ˂)", "(•ᴗ•)ღ",
    // actions / gestures
    "┬─┬ノ( º _ ºノ)", "(╯°□°)╯︵ ┻━┻", "(ﾉ°益°)ﾉ", "(ﾉ°Д°)ﾉ︵ ┻━┻", "(._.) ~ ┬─┬",
    "(∩｀-´)⊃━☆ﾟ.*･｡ﾟ", "o(´^`)o", "m(_ _)m", "orz", "_(:3 」∠)_", "(ᕗ ͜ʖ ᕗ)", "ᕕ( ᐛ )ᕗ", "ᕦ(ò_óˇ)ᕤ",
    "(ノ・∀・)ノ", "(´∩｡• ᵕ •｡∩`)", "(ง •̀_•́)ง", "(｡•̀ᴗ-)", "(ᗒᗣᗕ)", "(ﾉ´з｀)ノ", "(づ•ᴗ•)づ",
    "(⌒ω⌒)", "(⊃｡•́‿•̀｡)⊃", "(ノ_ヽ)", "(^人^)", "(人 •͈ᴗ•͈)", "(•‿•)", "(•ω•)", "(ᵕ—ᴗ—)",
    "(☕️•ᴗ•☕️)", "(｡･∀･)ﾉﾞ", "(^_^)/", "(｡･ω･｡)ﾉ", "(ㅅ´ ˘ `)", "(´｡• ω •｡`)",
).distinct()

/** Common two-emoji text combinations, inserted as ordinary Unicode text. */
private val EMOJI_PAIRS = listOf(
    // shy / cute
    "🥹👉👈", "🥺👉👈", "🥺🙏", "🥹🫶", "🥰🫶", "🫶💖", "💖✨", "😳👉👈", "🙈🙉", "🙈💕",
    // laughing / dead
    "😭💀", "💀🔥", "🤭💀", "😂💀", "😂😭", "🤣😭", "💀💀", "😭😭", "😭🙏", "😂🤣",
    // hype / vibe
    "🤌✨", "💅✨", "👑✨", "🔥🔥", "✨🌟", "💫✨", "🎉🥳", "🙌🔥", "💯🔥", "⚡🔥", "😎🤏", "😎👍",
    // love / friends
    "❤️‍🔥💞", "💔❤️‍🩹", "🤝❤️", "💕💕", "💞💗", "😍😘", "😘💋", "🥰💕", "🫂💖", "👫💕", "💑🌹", "💍💒",
    // greetings / thanks / ok
    "👋😊", "🙏❤️", "🫡🫶", "👍👍", "👌😌", "🤞🍀", "✌️😎", "🫰💸", "🙌🥳", "👏👏", "🤗💖", "🤝🤝",
    // animals
    "🐱💖", "🐶🦴", "🐻🍯", "🐼🎋", "🐰🥕", "🦊🍂", "🐸☕", "🦋🌸", "🐝🍯", "🐢🌊", "🦄🌈", "🐧❄️", "🐨🍃", "🐥🌱",
    // nature / weather
    "☀️🌈", "🌙✨", "🌙⭐", "🌧️☔", "⛈️⚡", "🌸🌷", "🌻☀️", "🍂🍁", "❄️⛄", "🌊🏄", "🏔️🌄", "🌅🏖️", "🪴🌱", "🌴🥥",
    // food / drink
    "🍓🍰", "🍕❤️", "🍔🍟", "🍣🥢", "🍜🥢", "🌮🌯", "🍩☕", "🍦🍨", "🧋🧋", "☕📖", "🍺🍻", "🥂🍾", "🍫🍬", "🍳🥓", "🥑🍞", "🍉🍹",
    // everyday / activities
    "🎧🎶", "📚☕", "🎂🎉", "🧠💡", "💃🕺", "🎮🕹️", "⚽🥅", "🏀🔥", "🏋️💪", "🧘🌿", "✈️🌍", "🏖️🍹", "🎬🍿", "📸✨", "🎨🖌️", "🎸🎤",
    "💻☕", "📱💬", "🔑🔒", "🎁🎀", "💰🤑", "🏠❤️", "🚗💨", "🚀🌙", "😴💤", "🤒🤧", "🥵🔥", "🥶❄️", "🤔💭", "🧐🔍", "😤💢", "😡🤬",
).distinct()

/** No tone first, then light to dark: the order of [EmojiTones.options]. */
private val TONE_LABELS = listOf(
    R.string.emoji_tone_none,
    R.string.emoji_tone_light,
    R.string.emoji_tone_medium_light,
    R.string.emoji_tone_medium,
    R.string.emoji_tone_medium_dark,
    R.string.emoji_tone_dark,
)

/** Centred above the anchor, kept inside the window horizontally. */
internal object AboveAnchor : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = anchorBounds.center.x - popupContentSize.width / 2
        return IntOffset(x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)), anchorBounds.top - popupContentSize.height)
    }
}
