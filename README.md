# Elara Keyboard

A privacy-conscious, fully offline Android keyboard. Part of the
Zenith's Space app ecosystem, but it is standalone:
it needs no other Zenith app.

- **Application ID:** `com.elara.keyboard`
- **Status:** early development (0.1.0). Not yet released.
- **Permissions:** only `VIBRATE`, for key vibration. No internet access.

## Features (0.1.0)

- QWERTY letters with a dedicated number row, a symbols page and a second symbols page (`=\<`).
- A comma key left of the emoji key.
- Shift: tap for one capital, double-tap for caps lock.
- Auto-capitalization: a capital at the start of a field, after a new line and after . ! or ?
  followed by a space, only in fields that ask for capitals (as Gboard does, so usernames and login
  fields stay lowercase). Off in password, email and URL fields; switch in Typing settings.
- Space after full stop: a full stop typed after a word brings its space. A digit or punctuation
  next takes the space back (3.14, e.g., ...); a space next is not doubled. Switch in Typing settings.
- Key-press preview bubble for characters.
- Long-press for accents and extra symbols: slide to pick, release to type.
- Glide typing: slide across the letters to type a word. Other matches show in the suggestion
  strip; backspace right after removes the glided word. Off in password fields. Can be turned off
  in Gestures settings.
- Backspace repeats while held. Slide left on it to delete a word, and further for more words
  (can be turned off in Gestures settings). Optionally, double tap backspace to delete a word
  (off by default, Gestures settings).
- The Enter key follows the field's action (Go, Search, Send, Next, Done) or inserts a new line.
- Number and phone fields open on the digit layer.
- Emoji panel: all Unicode emoji the device can draw, by category, with recently used first.
- Emoji search: tap the magnifier in the emoji panel and type on the letter keys. Matches emoji
  names and keywords, offline.
- Skin tones: long-press an emoji such as 👍 to pick a tone. The pick is remembered for that emoji.
- Recents can be cleared: "Clear all" (tap twice) between ABC and delete on the recents tab, or long-press one recent
  emoji and tap ✕ to remove just that one.
- Hold space for 1.5 seconds to change keyboard (Android's keyboard picker), once turned on in
  Gestures settings. Slide along space to move the cursor
  (can be turned off there).
- The space bar label can be hidden (Gestures settings).
- Word suggestions above the keys: completions and likely fixes, best in the middle. Tap one to
  type it. Off in password fields.
- One-handed mode from the toolbar: the keyboard narrows to one side; the strip beside it moves it
  to the other side or back to full width. The choice is remembered.
- A toolbar row above the keys (select all, cut, copy, paste, and a settings shortcut), shown
  in every field, with word suggestions on or off. While a word is being typed and suggestions
  are on, the same row shows the suggestions instead.
- Toolbar buttons are your choice: Look & feel > Toolbar switches each one (clipboard, select all,
  cut, copy, paste, one-handed) on or off. Settings always stays at the end. "Show toolbar"
  there hides the whole row.
- Clipboard history: the clock button in the toolbar opens recent copies. Tap one to paste,
  long-press to pin or delete. Clearing the whole history is in Clipboard settings. Just after a copy, the
  toolbar offers it as a one-tap paste for 5 minutes. Copied passwords are never kept.
  Retention (1 hour, 1 day, until removed) in Clipboard settings.
- Autocorrect on space or end punctuation, with the word it will type highlighted in the strip.
  Backspace right after undoes it and keeps your word. Off in email, URL and password fields.
- No app-drawer icon by default, so the keyboard stays out of the way. Open settings with
  **Open** on Elara Keyboard's App info page (or the installer), or the gear in the keyboard's
  toolbar. Look & feel > "Show in app drawer" adds the icon.
- Settings home: a hero card with the setup status (tap it for About), then four categories as
  rounded pills, each opening its own page. Typing (suggestions, autocorrect, capitals, spacing);
  Gestures (glide typing, backspace, space bar); Clipboard; Look & feel (theme, key height,
  key-press popup, sound, vibration). Setup sits below.
- Material 3 Expressive look: Elara brand purple, AMOLED-black dark mode, spring key-press morph.
- Key vibration (on by default) and key click sound (off by default), each also following the
  system setting.

Planned work is in [ROADMAP.md](ROADMAP.md).

## Install and enable

1. Install the APK (see Build below) and tap **Open** in the installer. Elara Keyboard has no
   app-drawer icon by default; later, open it from **Open** on its App info page
   (Settings > Apps > Elara Keyboard), or the gear in the keyboard's toolbar.
2. In **Setup**, tap **Enable keyboard** and turn on Elara Keyboard.
   Android shows a standard warning that any keyboard can see typed text.
   This app has no internet permission, so that text cannot leave the device.
3. Tap **Choose keyboard** and pick Elara Keyboard.
4. Optional: Look & feel > **Show in app drawer** adds a launcher icon.

To switch back, use the keyboard icon in the navigation bar, or turn on "Hold space to switch
keyboard" in Gestures settings and hold the space bar for 1.5 seconds.
Uninstalling the app also restores your previous keyboard.

## Build

Requirements: JDK 17 and the Android SDK with platform 37.

```sh
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # unit tests
./gradlew lintDebug              # Android Lint
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Debug builds use the application ID `com.elara.keyboard.dev` and show as "Elara Keyboard Dev", so
they install beside the release (`com.elara.keyboard`) without a signature clash. Enable it
separately under Settings, then pick it from the keyboard switcher.

### Signed release build

Release builds are signed only when the Gradle property `zenboardSigning` points to a
`keystore.properties` file kept outside this repository:

```properties
# ~/.gradle/gradle.properties
zenboardSigning=/path/to/keystore.properties
```

```properties
# keystore.properties
storeFile=/path/to/zenboard-release.jks
storePassword=...
keyAlias=zenboard
keyPassword=...
```

Then `./gradlew assembleRelease` writes `app/build/outputs/apk/release/app-release.apk`.
Without the property the release APK is unsigned. Never commit keystores or passwords.

## Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md)
- [PRIVACY.md](PRIVACY.md)
- [SECURITY.md](SECURITY.md)
- [CONTRIBUTING.md](CONTRIBUTING.md)
- [ROADMAP.md](ROADMAP.md)

## Third-party data

`app/src/main/assets/emoji.txt` is generated from Unicode's `emoji-test.txt` (Emoji 18.0),
© Unicode, Inc., used under the [Unicode License v3](https://www.unicode.org/license.txt).
`app/src/main/assets/emoji_keywords.txt` (search names and keywords) is generated from the same
file and the English CLDR annotations (`annotations/en.xml`, `annotationsDerived/en.xml`), under
the same license. `app/src/main/assets/emoji_tones.txt` (skin-tone variants) is generated from
`emoji-test.txt` as well.

`app/src/main/assets/dict_en_US.txt` is generated from AOSP LatinIME's
`dictionaries/en_US_wordlist.combined` (60,000 most frequent words, offensive-flagged entries
removed), © The Android Open Source Project, [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).

### Publishing a release

`.github/workflows/release.yml` runs when a tag `keyboard-v<versionName>` is pushed (for example
`keyboard-v1.2.0`). It checks the app ID and that the tag matches `versionName`, runs the unit
tests and lint, builds the signed release, and publishes it as a GitHub release with the APK and
its SHA-256 checksum. It needs four repository secrets:

| Secret | Value |
|---|---|
| `KEYBOARD_KEYSTORE_BASE64` | The keystore file, base64-encoded (`base64 -w0 release.jks`) |
| `KEYBOARD_STORE_PASSWORD` | The keystore's password |
| `KEYBOARD_KEY_ALIAS` | The key's alias |
| `KEYBOARD_KEY_PASSWORD` | The key's password |

Use the same key as your local release builds, or phones with the app installed can't update.

## License

Apache License 2.0; see [LICENSE](LICENSE). Third-party data and its licences are listed in
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
