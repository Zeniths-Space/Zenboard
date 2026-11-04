# Zenboard

A privacy-conscious, fully offline Android keyboard. Part of the
Zenith's Space app ecosystem, but it is standalone:
it needs no other Zenith app.

- **Application ID:** `app.zenith.zenboard`
- **Status:** early development (0.1.0). Not yet released.
- **Permissions:** only `VIBRATE`, for key vibration. No internet access.

## Features (0.1.0)

- QWERTY letters, a symbols/digits page and a second symbols page (`=\<`).
- A comma key left of the emoji key.
- Shift: tap for one capital, double-tap for caps lock.
- Auto-capitalization that follows the text field's settings.
- Key-press preview bubble for characters.
- Long-press for accents, digits (top row) and extra symbols: slide to pick, release to type.
- Backspace repeats while held. Slide left on it to delete a word, and further for more words.
- The Enter key follows the field's action (Go, Search, Send, Next, Done) or inserts a new line.
- Number and phone fields open on the digit layer.
- Emoji panel: all Unicode emoji the device can draw, by category, with recently used first.
- Emoji search: tap the magnifier in the emoji panel and type on the letter keys. Matches emoji
  names and keywords, offline.
- Skin tones: long-press an emoji such as 👍 to pick a tone. The pick is remembered for that emoji.
- Recents can be cleared: "Clear all" (tap twice) between ABC and delete on the recents tab, or long-press one recent
  emoji and tap ✕ to remove just that one.
- Hold space to change keyboard (Android's keyboard picker). Slide along space to move the cursor.
- Word suggestions above the keys: completions and likely fixes, best in the middle. Tap one to
  type it. Off in password fields.
- When no word is being typed, the suggestion strip holds a toolbar: select all, cut, copy,
  paste, and a settings shortcut.
- Autocorrect on space or end punctuation, with the word it will type highlighted in the strip.
  Backspace right after undoes it and keeps your word. Off in email, URL and password fields.
- Settings by category, each on its own page: Setup; Appearance (pure black, key height,
  key-press popup); Typing (word suggestions, autocorrect); Sound and vibration; About.
- Material 3 Expressive look: wallpaper colours, AMOLED-black dark mode, spring key-press morph.
- Key vibration (on by default) and key click sound (off by default), each also following the
  system setting.

Planned work is in [ROADMAP.md](ROADMAP.md).

## Install and enable

1. Install the APK (see Build below).
2. Open **Zenboard** from the launcher.
3. Tap **Open keyboard settings** and turn on Zenboard.
   Android shows a standard warning that any keyboard can see typed text.
   This app has no internet permission, so that text cannot leave the device.
4. Tap **Choose keyboard** and pick Zenboard.

To switch back, hold the space bar (or use the keyboard icon in the navigation bar).
Uninstalling the app also restores your previous keyboard.

## Build

Requirements: JDK 17 and the Android SDK with platform 37.

```sh
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # unit tests
./gradlew lintDebug              # Android Lint
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Debug builds use the application ID `app.zenith.zenboard.dev` and show as "Zenboard Dev", so
they install beside the release (`app.zenith.zenboard`) without a signature clash. Enable it
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

## License

Not yet selected. It will be chosen before the first public release.
