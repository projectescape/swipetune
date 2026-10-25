# SwipeTune

SwipeTune is a private, local-first Android music player with a swipe-based library review mode.

## Features

- Pick any music folder with Android's system folder picker.
- Play local MP3, FLAC, M4A, AAC, WAV, and OGG files.
- View embedded album art, title, artist, and album metadata.
- Material 3 Expressive interface with dynamic color, album-art atmosphere, frosted floating controls, and spring motion.
- Play/pause, seek, and jump backward or forward ten seconds.
- Review songs in a persisted random, non-repeating order.
- Swipe right to keep and left to reject.
- Undo returns to the previous song and reverses its decision.
- Rejected songs are hidden from the normal music library without changing their files.
- Restore rejected songs, or permanently delete them only after an explicit confirmation.
- No account, server, analytics, or collection-specific data.

## Build

Requirements: JDK 17, Android SDK 37, and build tools 36.0.0.

```bash
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
mise exec -- ./gradlew lintDebug test assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

