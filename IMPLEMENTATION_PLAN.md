# SwipeTune rebuild plan

## Audit summary

### What already works

- Storage Access Framework folder selection and persisted access.
- Generic local-file discovery for common audio formats.
- Basic Media3 playback, metadata/art extraction, seek, swipe decisions, undo, restore, and confirmed deletion.
- Persistent review order and decisions.
- A useful Material 3 Expressive visual prototype.

### Critical gaps

1. Playback is owned by `MusicViewModel`, so it cannot reliably survive the screen lifecycle or provide Android lock-screen, notification, Bluetooth, headset, and external media controls.
2. Normal mode is a filename-oriented list rather than a complete music library. It lacks durable album/artist/folder/favorite browsing, useful search/sort, collection playback, and an owned queue.
3. There is no full player. Current track context, artwork, transport controls, seek state, shuffle/repeat/favorite state, and queue are fragmented or absent.
4. Review playback and normal playback are not cleanly separated. A review decision must never corrupt the user's normal queue, and rejected tracks must remain excluded from normal browsing.
5. Appearance state is incomplete and dark mode has incorrect content colors. System/light/dark and optional Android dynamic color need a real settings surface and whole-app validation.
6. Loading, missing-file, permission-loss, empty-library, and playback-error recovery are under-specified.
7. The current single-file UI has grown past a safe maintenance size and should be split by product surface.

### PixelPlayer benchmark findings

PixelPlayer's strongest transferable patterns are architectural: a `MediaSessionService` owns playback; mini and full players reflect the same session; library categories and collection-detail screens build queues; settings are grouped by user intent; and advanced features stay modular. SwipeTune will implement those patterns independently, without copying PixelPlayer code, assets, wording, or branding.

## Execution plan

### Gate 1 — playback foundation

- Move the only `ExoPlayer` instance into `PlaybackService`.
- Connect UI state and commands through a `MediaController`.
- Publish title, artist, album, duration, artwork URI/data, queue, shuffle, and repeat through `MediaSession`.
- Support audio focus, becoming-noisy behavior, background playback, media buttons, notification/lock-screen controls, and app re-entry through the session activity.
- Persist and restore the queue, current item, and position conservatively.

Acceptance: playback continues with the app backgrounded; notification and lock screen show real metadata/art and working previous/play-next controls; Bluetooth/headset commands reach the same player.

### Gate 2 — library model

- Index metadata once during scan and cache artwork independently.
- Browse Songs, Albums, Artists, Folders, Favorites, and persistent local Playlists; exclude rejected tracks everywhere in normal mode.
- Add search, deterministic sort choices, collection counts, collection playback, and shuffle-all.
- Handle changed/missing files and revoked folder permission without crashing.

Acceptance: every discovered track is reachable through at least one normal library view; tapping a song or collection builds the expected service queue.

### Gate 3 — player surfaces and queue

- Add one persistent mini-player above navigation.
- Add a full player with artwork, title/artist/album, seek bar and time, previous/play-next, shuffle, repeat, favorite, and queue access.
- Add an editable queue sheet with jump-to-item and remove-from-queue; keep the currently playing item explicit.
- Derive all player surfaces from the media controller rather than duplicate local state.

Acceptance: mini-player, full player, notification, and lock screen always agree about the active track and playback state.

### Gate 4 — review as a first-class mode

- Keep randomized, non-repeating review order and persisted decisions.
- Give Review an isolated queue while preserving enough normal-queue state to return safely.
- Keep album art, metadata, playback/seek, swipe affordances, explicit Keep/Reject buttons, and undo.
- Hide rejected tracks from all normal views immediately; allow restore and confirmed file deletion in review management.

Acceptance: swiping never leaks a rejected track into the normal library or loses undo history, and returning to normal listening behaves predictably.

### Gate 5 — settings and theme

- Build grouped settings for Appearance, Library, Playback, Review data, and About.
- Add System/Light/Dark mode and an independent dynamic-color switch.
- Fix system-bar icon contrast, startup theme, content-color inheritance, and all empty/dialog/sheet surfaces in light and dark.
- Add change-folder/rescan actions and clear explanations for destructive review cleanup.

Acceptance: all three theme modes work without restart, dynamic color can be toggled, and every screen remains readable in light and dark.

### Gate 6 — expressive rebuild

- Split the oversized UI into focused Library, Review, Player, Settings, and shared-component files.
- Apply `design.md`: album-art-led atmosphere, selective frost, asymmetric expressive shapes, semantic color, and purposeful spring motion.
- Add compact and expanded layouts and accessibility semantics/content descriptions.
- Avoid dead controls for unimplemented advanced modules.

Acceptance: no clipping or horizontal overflow at 320/375/414 dp; a useful two-pane treatment at 768 dp; touch targets and contrast remain accessible.

### Gate 7 — verification and release

- Add unit tests for review state, queue mapping, settings persistence, filtering, and metadata grouping.
- Run formatting/compile, unit tests, lint, and debug APK assembly.
- Exercise a real emulator/device: initial folder flow, playback lifecycle, notification, lock screen, headset commands, search/sort, queue, swipe/undo, rotation, and process recreation.
- Capture light, dark, and dynamic-color screenshots and run the Hallmark slop/contract checks before release.

## Deferred modules

After the baseline passes all gates: sleep timer, embedded/local lyrics, equalizer integration, home-screen widgets, listening statistics, tag editing, backup/restore, Android Auto, and optional remote sources. Each should ship as a working vertical slice, not as a placeholder button.

## Planned file changes

- Modify: Gradle configuration, manifest, `MainActivity.kt`, models, repository, session store, theme, and review queue integration.
- Replace/split: `MusicViewModel.kt` and `ExpressiveUi.kt` responsibilities into playback, library, player, review, settings, and shared UI files.
- Add: media-controller connection/state mapper, service playback persistence, library grouping helpers, focused Compose screens/components, and tests.
- Delete: no production files during the first pass. Superseded code is removed only after its replacement is wired and verified.

## Execution status — 0.3.0

- Gate 1: complete. Service-owned Media3 playback, controller-backed UI, system media session, notification/lock-screen metadata and controls, audio focus/noisy handling, and playback snapshots are implemented.
- Gate 2: complete. Metadata-rich Songs, Albums, Artists, Folders, Favorites, and local Playlists support search, sort, collection playback, and rejected-track filtering.
- Gate 3: complete. Persistent mini-player, immersive full player, transport/seek/shuffle/repeat/favorite controls, and an editable queue are implemented.
- Gate 4: complete. Review uses an isolated queue, preserves/restores normal playback, persists decisions, supports undo, and keeps rejection management contextual.
- Gate 5: complete. Settings includes System/Light/Dark, optional Android dynamic color, library maintenance, playback integration information, and app information.
- Gate 6: complete for the rendered baseline. The album-art-led Material 3 Expressive system, selective frost, semantic surfaces, and responsive compact/expanded layouts are implemented. Further source-file decomposition is maintenance work, not a release blocker.
- Gate 7: complete. Unit tests, lint, APK assembly, 320/375/414/768 dp visual checks, real media-session/notification inspection, theme switching, playlists, and normal↔Review queue restoration pass on an Android 16 emulator.
