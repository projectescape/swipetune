# SwipeTune

SwipeTune is a private, local-first Android music player with a swipe-based library review mode.

## Features

- Pick any music folder with Android's system folder picker.
- Play local MP3, FLAC, M4A, AAC, WAV, and OGG files.
- View embedded album art, title, artist, and album metadata.
- Browse songs, albums, artists, genres, folders, favorites, playback history, and persistent local playlists.
- Search and sort the library, play collections, and shuffle the visible library.
- Use a persistent mini-player or an immersive full player with seek, previous/next, shuffle, repeat, favorite, and queue controls.
- Continue playback in the background with Android lock-screen, notification, Bluetooth, and headset controls through Media3.
- Resume the last queue, track, position, shuffle state, and repeat state after service recreation.
- Reorder the live queue, play next, append songs, undo queue edits, clear upcoming tracks, or save the queue as a playlist.
- Browse the same library from Android Auto and control playback from the system media player, lock screen, notification shade, Bluetooth, a home-screen widget, and headset controls.
- Use local synchronized LRC lyrics, a service-owned sleep timer, audio-session equalizer, bass boost, loudness, ReplayGain, gapless/offload preferences, and optional fade transitions.
- Create ordered playlists with custom covers, duplicate/merge/quick-fill them, and import or export M3U/M3U8 playlists.
- Select multiple tracks for batch queue, favorite, or Review actions; find likely duplicate recordings without deleting them automatically.
- Keep playback history and restore playlists, Review decisions, preferences, and history with Android Auto Backup or a portable ZIP backup.
- Choose System, Light, or Dark appearance and independently enable Android dynamic color.
- Material 3 Expressive interface with dynamic color, album-art atmosphere, frosted floating controls, and spring motion.
- Play/pause, seek, and jump backward or forward ten seconds.
- Review songs in a persisted random, non-repeating order.
- Swipe right to keep and left to reject.
- Undo returns to the previous song and reverses its decision.
- Rejected songs are hidden from the normal music library without changing their files.
- Entering Review temporarily preserves the normal playback queue and restores it on exit.
- Restore rejected songs, or permanently delete them only after an explicit confirmation.
- No account, server, analytics, or collection-specific data.

## Build

Requirements: JDK 17, Android SDK 37, and build tools 36.0.0.

```bash
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
mise exec -- ./gradlew lintDebug test assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

