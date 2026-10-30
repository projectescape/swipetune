# Foreground service declaration draft

## Type

`mediaPlayback`

## Functionality

SwipeTune uses a media playback foreground service to continue user-initiated local audio playback when the app is backgrounded. It also exposes transport controls and playback metadata to Android's notification shade, lock screen, Bluetooth and headset controls, Android Auto, and compatible system media surfaces.

## Why the task must start immediately

Playback must begin immediately after the user taps a song or playback control. Deferring the task would make the requested audio appear broken or unresponsive.

## User impact if interrupted

Interrupting the service stops or disrupts audio the user explicitly chose to play and removes the expected system playback controls.

## User control

Playback is always user-initiated and can be paused or stopped from SwipeTune, the notification, lock screen, Bluetooth or headset controls, Android Auto, or Android system media controls.

## Required demonstration video

Record a short unlisted video showing:

1. The user taps a song in the SwipeTune library.
2. Audio playback starts and the mini player updates.
3. The app is backgrounded.
4. Playback continues and Android's media notification or lock-screen control appears.
5. The user pauses playback from the system control.
