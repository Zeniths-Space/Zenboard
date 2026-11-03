# Privacy

Zenith Keyboard is built so that what you type stays on your device.

- **No internet permission.** The app cannot open network connections.
- **No telemetry, analytics, ads, or accounts.**
- **No storage of typed text.** Keys go straight to the app you are typing in.
  The keyboard keeps no typing history.
- **Recently used emoji** (up to 32) are kept in app-private storage on the device, so the
  emoji panel can show them first. Nothing is recorded in password fields, or when the app you
  type in asks for incognito (`IME_FLAG_NO_PERSONALIZED_LEARNING`). Clearing the app's storage
  removes them.
- **No logging** of typed content.
- **Backups:** disabled (`allowBackup="false"`), so recent emoji never leave the device.

Future features that store data on the device, such as a personal dictionary or
learned words, will be documented here before release. They will skip password
and other sensitive fields.
