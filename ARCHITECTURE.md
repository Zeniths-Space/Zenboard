# Architecture

One Gradle module (`app`). It will be split only when complexity justifies it.

## Files

| File | Role |
|---|---|
| `KeyboardModel.kt` | Pure logic: layouts, shift/caps state machine, Enter action, key widths. No framework calls, so it is unit-tested on the JVM. |
| `ZenboardService.kt` | `InputMethodService`. Owns the keyboard state and turns key presses into `InputConnection` calls. |
| `KeyboardView.kt` | Compose UI for the keys. Stateless: it renders `KeyboardState` and reports `Key` presses. |
| `SettingsActivity.kt` | Launcher entry and the IME's settings activity: category home, Appearance, Typing and Sound and vibration pages, page switching. |
| `SetupScreen.kt` / `AboutScreen.kt` | Setup steps page; about page with version, privacy, license status and credits. |
| `SettingsComponents.kt` | Shared grouped tiles, switch and slider tiles, page header, scallop badge, try-it field. |
| `SettingsStore.kt` | DataStore-backed settings, observed live by the keyboard. |
| `Feedback.kt` | Key vibration and click sound, switched by settings. |
| `Theme.kt` | Theme: wallpaper colours with the AMOLED / tinted-page surface treatment, semibold type, motion tokens. |
| `KeyPreview.kt` | Key-press preview bubble, drawn in one overlay window. |
| `EmojiData.kt` / `EmojiPanel.kt` | Emoji list (asset), recents, and the tabbed emoji grid. |
| `Dictionary.kt` | Word list (sorted asset, binary search), completions, typo candidates with typing-aware edit distance, suggestion ranking. |
| `SuggestionStrip.kt` | The three-slot strip above the keys. |
| `Shapes.kt` | Scallop badge shape, stadium, grouped-list corners. |
| `KeyIcons.kt` | Vector icons (Material paths plus hand-drawn shift and globe). |

## Data flow

```
touch -> KeyboardView -> onKey(Key) -> ZenboardService
                                         |-> InputConnection (commitText / key events / editor action)
                                         '-> KeyboardState (Compose state) -> KeyboardView recomposes
editor cursor moves -> onUpdateSelection -> auto-caps check -> KeyboardState
```

## Decisions

- **Compose inside an IME.** The service implements `LifecycleOwner` and
  `SavedStateRegistryOwner`, and sets them on the IME window's decor view, so
  `ComposeView` works outside an Activity.
- **Backspace and Enter send key events**, not `deleteSurroundingText`. Key events
  delete a selection or a whole emoji, and they work in fields such as terminals.
  A tap deletes on release, so a slide left can become a word delete instead without
  first removing a character. Holding starts the repeat after 400 ms. Word delete
  (one word per 40 dp of slide) uses `deleteSurroundingText` back to the previous space.
- **Space-bar cursor movement uses `setSelection`**, not arrow-key events: at either end of
  a field an arrow key moves focus to another view. The service keeps the cursor position
  itself and ignores the late `onUpdateSelection` echoes of its own moves, so a fast slide
  does not roll back.
- **Auto-caps is event driven.** It is checked on `onUpdateSelection`, and the IPC
  call is skipped for fields with no capitalization flags. One-shot shift is
  cleared synchronously after a character, so fast typing never doubles a capital.
- **System-bar insets are read on the IME decor view.** A framework parent with
  `fitsSystemWindows` stops inset dispatch before the ComposeView, so
  `navigationBarsPadding()` does nothing. On Android 15+ the IME's own nav-bar strip
  (back and switch-keyboard buttons) is reported as `captionBar`. The service reads
  `systemBars()` insets, which include it, and pads the keyboard with them.
  Verified on Android 17 (Galaxy S24 Ultra).
- **Key previews use one overlay window.** It is a Compose `Popup`: a sub-panel of the IME window,
  `FLAG_NOT_TOUCHABLE`, covering the keyboard plus 72dp above it. It exists only between
  `onStartInputView` and `onFinishInputView` and is reused for every press, so typing never adds or
  removes windows, and touches (including fast rollover typing) pass straight to the keys.
  Positions are IME-window-relative, so `FLAG_LAYOUT_IN_SCREEN` must not be set.
- **Long-press alternates** open after the system long-press timeout (the user's accessibility
  setting), in the same overlay window as the previews. Layout and selection maths
  (`altLayout`, `altIndexAt`) are pure and unit-tested. Near the right edge the row flips, so the
  first option is always above the key. Finger x is mapped to the root through the key's own
  coordinates, because the press-scale layer would otherwise skew it. Screen readers get each
  alternate as a custom accessibility action.
- **Emoji without a library.** `androidx.emoji2:emoji2-emojipicker` was considered and rejected.
  It pulls Guava, AppCompat and RecyclerView into an always-resident IME process, and starts a
  downloadable-font fetch at app startup. Instead, `assets/emoji.txt` (16 KB) is generated from
  Unicode `emoji-test.txt`: fully-qualified emoji, skin-tone variants omitted. It is loaded off the
  main thread on first use, and `Paint.hasGlyph` drops emoji the device font cannot draw. The panel
  takes the height of the three upper rows, so the keyboard does not resize. Its bottom row is
  only ABC (left) and delete (right).
- **Emoji search** reads `assets/emoji_keywords.txt` (90 KB: Unicode name and English CLDR
  keywords per emoji), loaded on the first search only. While searching, the letter rows return
  and keys edit the query held in `ZenboardService` instead of the editor; nothing typed there
  reaches the app. Matching (`searchEmoji`) is a pure word-prefix scan, unit-tested: about 1,900
  entries, fast enough per keystroke without an index.
- **Skin tones** come from `assets/emoji_tones.txt` (28 KB: base emoji and its five single-tone
  variants), loaded with the emoji list. Long-press opens the tone row in a Compose `Popup` above
  the cell, unclipped so it can rise over the keyboard top. The last pick per emoji is kept in the
  same private preferences as recents, under the same incognito rule. Two-tone combinations (such
  as a handshake with two different tones) are not offered.
- **Changing keyboard** is a long-press on space, which calls `showInputMethodPicker()`. The
  emoji key took the old globe key's place.
- **Settings live in DataStore** (`datastore-preferences`, Apache-2.0, as the project brief specifies).
  Both the settings screen and the IME collect the same `Flow`, so a toggle changes the open
  keyboard at once. An unreadable store falls back to the defaults. The key-height slider saves once,
  when the finger lifts, not on every frame of the drag.
- **Key feedback** (`Feedback.kt`) goes through one `KeyFeedback` object provided as a
  CompositionLocal. Vibration calls the `Vibrator` directly with predefined click and tick effects
  and `USAGE_TOUCH`, so it follows the system touch-vibration setting. `View.performHapticFeedback`
  is not used: on Samsung (One UI, Android 17) an IME's calls are filed under usage `UNKNOWN`,
  whose intensity is off, so they were silently dropped. This needs the `VIBRATE` permission.
  Sound uses `AudioManager.playSoundEffect` with the key-click effects. It plays at the System
  volume and is muted when the phone is on silent or vibrate.
- **Pure black is opt-in.** Dark mode uses the wallpaper scheme's own surfaces by default. Pure black
  swaps in the near-black AMOLED ladder.
- **No navigation library.** The settings home lists the categories (Setup, Appearance, Typing,
  About), each with a one-line status. Each category is its own page. The activity switches pages
  with an `AnimatedContent` and `BackHandler`.
- **Suggestions use composing text.** The word being typed is the editor's composing region
  (underlined), so a suggestion replaces it in place. The first letter picks up any letters already
  left of the cursor, so the whole word is composed. Typing inside a word stays plain. A cursor jump
  out of the composing region drops it. The dictionary loads on the first field that allows
  suggestions, off the main thread. It is one array sorted by lowercase word, with binary search
  for lookups and prefix ranges and no hash map, to keep the IME's memory small. Fixes (edit
  distance: apostrophe 0.2, doubled letter 0.4, swap 0.5, neighbouring key 0.6, other 1.0) and
  completions (0.3 per letter still to type) are ranked on one score.
- **Autocorrect** applies on space and `. , ? ! : ;` when the best fix is confident: one weighted
  edit, or up to 1.6 in words of five or more letters. A single letter is corrected only for
  "i" -> "I". The next backspace undoes it if the text still ends with the correction and its
  separator. The original word is then learned, kept in memory and in `LearnedWords` unless the
  field is incognito.
- **Accent colour** is the scheme's secondary (Enter, caps lock, press tint, previews, selection).
- **Row height** is min(54dp, 9% of screen height), so landscape keeps the app visible.
- **No dependency injection framework and no navigation library.** They are not needed at this size.
- **Design language** matches the owner's Kontract app (M3 Expressive): near-black surface
  ladder in dark mode, white keys on a tinted page in light mode, slate ink, uniform 10dp key
  corners (Enter included), scallop badges on the settings pages, and a spring press morph (key rounds
  toward a pill and dips 5%). Light-mode function keys use `surfaceDim`, because the nested-card
  tone is nearly the page colour. The service sets light/dark nav-bar appearance on the IME
  window so the system's back and switch-keyboard buttons stay visible.
- **No icon or shapes libraries.** About a dozen icons are inline vector paths, and the scallop is a polar sine
  path. That avoids `material-icons-extended` and `graphics-shapes`.
- **Theme:** plain `MaterialTheme`. `MaterialExpressiveTheme` is not public in
  stable material3 1.4.0; switch when 1.5.0 is stable.
