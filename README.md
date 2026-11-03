# Zenboard

A privacy-conscious, fully offline Android keyboard. Part of the
Zenith's Space app ecosystem, but it is standalone:
it needs no other Zenith app.

- **Application ID:** `app.zenith.zenboard`
- **Status:** early development (0.1.0). Not yet released.
- **Permissions:** none. No internet access.

## Features (0.1.0)

- QWERTY letters and a symbols/digits layer.
- Shift: tap for one capital, double-tap for caps lock.
- Auto-capitalization that follows the text field's settings.
- Key-press preview bubble for characters.
- Long-press for accents, digits (top row) and extra symbols: slide to pick, release to type.
  Comma is the first long-press option on the period key.
- Backspace repeats while held.
- The Enter key follows the field's action (Go, Search, Send, Next, Done) or inserts a new line.
- Number and phone fields open on the digit layer.
- Emoji panel: all Unicode emoji the device can draw, by category, with recently used first.
- Hold space to change keyboard (Android's keyboard picker).
- Settings by category: Setup, Appearance (pure black, off by default), Typing (key-press
  popup, on by default) and About, each on its own page.
- Material 3 Expressive look: wallpaper colours, AMOLED-black dark mode, spring key-press morph.
- Haptic feedback that follows the system setting.

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

## Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md)
- [PRIVACY.md](PRIVACY.md)
- [SECURITY.md](SECURITY.md)
- [CONTRIBUTING.md](CONTRIBUTING.md)
- [ROADMAP.md](ROADMAP.md)

## Third-party data

`app/src/main/assets/emoji.txt` is generated from Unicode's `emoji-test.txt` (Emoji 18.0),
© Unicode, Inc., used under the [Unicode License v3](https://www.unicode.org/license.txt).

## License

Not yet selected. It will be chosen before the first public release.
