# Roadmap

## 0.1 — Usable base (current)
- [x] QWERTY and symbols layers, shift and caps lock, auto-caps
- [x] Backspace repeat, Enter editor actions, next-keyboard key
- [x] Settings by category: Setup, Appearance, Typing and About pages
- [x] Real-device check on Galaxy S24 Ultra, Android 17 (2026-11-03): typing, shift and caps lock, auto-caps, held backspace, Next action, digit layer for phone fields, newline, landscape, next-keyboard key
- [ ] TalkBack check

## 0.2 — Typing comfort
- [x] Key-press preview popup
- [x] Long-press for accented letters and alternate symbols
- [x] Second symbols page (`=\<` etc.)
- [x] Swipe left on backspace to delete a word
- [x] Space-bar cursor movement
- [x] Settings: pure black, key-press popup
- [x] Settings: key height, haptics, sound

## 0.2 — also done
- [x] Emoji panel with categories and recents
- [x] Hold space to change keyboard

## Later (emoji)
- [x] Skin-tone picker (long-press an emoji)
- [x] Emoji search

## 0.3 — Suggestions
- [x] On-device word suggestions (no suggestions in password fields)
- [x] Autocorrection, with backspace to undo
- [x] Learned words, stored locally (from undone corrections)
- [ ] Personal dictionary editor

## Later
- [ ] More layouts and languages
- [x] Clipboard history within Android's restrictions (pin, delete, retention)
- [x] Themes (six presets and an accent colour)
- [x] Swipe typing (glide typing; shape matching, no language model yet)
- [x] Signed release workflow (tag `keyboard-v*`); first GitHub Release waits for the signing secrets
- [x] License: Apache-2.0

## Battery and speed (2026-11-12)
- [x] Key-preview window no longer polled every frame while the keyboard is idle: 4.6 s of CPU in 30 s idle down to 0 (debug build, S24 Ultra)
- [x] Haptic effects built once; lighter preview shadow
- [x] Learned words drop the least recently used past 1,000, not an arbitrary subset
- [ ] Overnight idle drain (Battery Historian) and a signed-release-build CPU/jank run
- [ ] Layer-switch cost: ~6 ms of per-key recomposition still open
- [ ] Swipe typing language model: needs a bundled word-pair dataset (we do not store what is typed)
