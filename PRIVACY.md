# Privacy

Zenboard is built so that what you type stays on your device.

- **No internet permission.** The app cannot open network connections.
- **No telemetry, analytics, ads, or accounts.**
- **No storage of typed text.** Keys go straight to the app you are typing in.
  The keyboard keeps no typing history.
- **Recently used emoji** (up to 32) are kept in app-private storage on the device, so the
  emoji panel can show them first. Nothing is recorded in password fields, or when the app you
  type in asks for incognito (`IME_FLAG_NO_PERSONALIZED_LEARNING`). Clearing the app's storage
  removes them.
- **Clipboard history** (on by default, Clipboard settings): text you copy is kept so you can
  paste it again. It is stored only on the device, in no-backup app storage
  (`noBackupFilesDir/clipboard.json`). Unpinned clips are deleted after 1 hour by default (or 1 day,
  or kept until removed, up to 30); pinned clips stay until you remove them. Text that Android or
  the copying app marks as sensitive (`ClipDescription.EXTRA_IS_SENSITIVE`, set by password
  managers) is never kept, and nothing is recorded while you type in an incognito field. Only plain
  text is kept, never links to files or other content. Turning history off deletes the unpinned
  clips at once; Clipboard settings can also clear unpinned clips, or every clip. Android lets only the active
  keyboard and the focused app read the clipboard.
- **No logging** of typed content.
- **Learned words** (up to 1,000): a word is learned only when you undo an autocorrect or tap the
  word you typed in the suggestion strip. Learned words are stored in app-private storage so they
  are not corrected again. They are never learned from password or incognito fields. No other typing
  is recorded.
- **Settings** (theme, key height, popup, vibration, sound, suggestions, autocorrect) are stored in app-private storage on the device.
- **Backups:** disabled (`allowBackup="false"`), so recent emoji and clipboard history never leave
  the device.

Future features that store data on the device, such as a personal dictionary or
learned words, will be documented here before release. They will skip password
and other sensitive fields.
