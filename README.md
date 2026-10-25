# SwipeTune

SwipeTune is an open-source, local-first Android music player with an optional swipe-based library review mode.

## Features

- Automatically index the music Android exposes on the device, refresh when MediaStore changes, and optionally add another folder through the system folder picker.
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
- Use non-deletable Accepted and Rejected smart playlists that update from Review decisions; Rejected is the deliberate place to inspect tracks hidden elsewhere.
- Select multiple tracks for batch queue, favorite, or Review actions; find likely duplicate recordings without deleting them automatically.
- Keep playback history and restore playlists, Review decisions, preferences, and history with Android Auto Backup or a portable ZIP backup.
- Choose System, Light, or Dark appearance and independently enable Android dynamic color.
- Material 3 Expressive interface with dynamic color, album-art atmosphere, frosted floating controls, spring motion, and display-cutout-safe edge-to-edge layouts.
- Play/pause, seek, and jump backward or forward ten seconds.
- Start Review deliberately from Settings, targeting all unreviewed music or one indexed folder.
- Resume the same persisted, non-repeating Review session after leaving or restarting the app.
- Swipe right to keep and left to reject.
- Undo returns to the previous song and reverses its decision.
- Rejected songs are hidden from the normal music library without changing their files.
- Inspect accepted and rejected decisions and clear either decision from Review settings.
- Entering Review temporarily preserves the normal playback queue and restores it on exit.
- Restore rejected songs, or permanently delete them only after an explicit confirmation.
- No account, server, analytics, or collection-specific data.

## License

SwipeTune is free software licensed under the GNU General Public License v3.0. It includes an FFmpeg decoder distributed through Jellyfin's GPLv3 Media3 extension. See `LICENSE` and the corresponding source in this repository. The software is provided without warranty.

## Build

Requirements: JDK 17, Android SDK 37, and build tools 36.0.0.

```bash
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
mise exec -- ./gradlew lintDebug test assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

