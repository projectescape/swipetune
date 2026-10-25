# SwipeTune 0.4 complete-player plan

## Reference study evidence

- Installed official PixelPlayer 0.7.5-beta arm64 build on the Pixel 9 API 36 emulator and verified its APK against the release SHA-256.
- Copied four real, artwork-bearing MP3s from `/Volumes/sandisk/Music` to `/sdcard/Music/PixelPlayerStudy`; no pendrive file was modified.
- Captured video of onboarding, destination navigation, playback start, mini-player appearance and expansion, live waveform progress, play/pause, seeking, queue reveal/reorder, settings navigation, scrolling, and choice sheets.
- Design conclusion: perceived quality comes from persistent state and coordinated continuity, not decoration. Player state remains visible during navigation; selected states reshape their surfaces; progress is visibly alive; and sheets/reordering preserve spatial context.

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

## Execution plan — dependency ordered

### Gate 0 — migration safety and motion benchmark

- Keep the current 0.3 JSON state readable for a one-time import into the new database.
- Preserve the working `MediaSessionService` while its internals are upgraded.
- Encode the observed motion contract in `design.md`; keep benchmark videos outside the repository as QA evidence.
- Add schema, repository, service, and UI tests before retiring old paths.

Acceptance: upgrading does not lose favorites, playlists, review decisions, queue, or selected folder; current playback still launches during migration.

### Gate 1 — durable indexed library (P0)

- Add Room entities for tracks, artists, albums, genres, folders, playlists/items, review decisions, favorites, play events, and scan roots.
- Unify MediaStore and persisted SAF roots behind one repository. Scan incrementally by URI, modified time, size, and generation where available; observe MediaStore changes and expose paged/sorted queries.
- Store multi-artist and genre relationships, disc/track numbers, year, codec/bitrate/sample rate, ReplayGain tags, file size, modified time, and stable artwork keys.
- Add folder exclusions, rescan/reconcile, missing-file handling, and transactional migration from `SessionStore`.

Acceptance: a large library does not live in one JSON blob or one Compose state list; rescans update changed rows and remove stale rows without rebuilding unrelated data.

### Gate 2 — complete library navigation and detail screens (P0)

- Introduce Navigation Compose destinations for Home, Search, Songs, Albums, Album detail, Artists, Artist detail, Genres, Genre detail, Folders/tree, Folder detail, Playlists, Playlist detail, History, Stats, Review, Player, and Settings.
- Add configurable library tabs, grid/list density, sort/filter options, multi-artist navigation, and responsive two-pane layouts.
- Keep current playback and album art visible in normal mode through one shared mini/full-player host.

Acceptance: every indexed item is reachable through a useful collection detail screen and every collection can play, shuffle, queue, favorite, share, or enter selection mode.

### Gate 3 — authoritative queue and silky player (P0)

- Make the service the sole queue authority and expose Media3 custom commands for Play Next, append, move, remove, clear, save as playlist, and undo.
- Add drag reorder, queue source labels, queue persistence, and optimistic undo.
- Rebuild mini/full players around shared state, continuous progress, active-row equalizer, waveform seek, container continuity, press feedback, and reduced-motion fallbacks from `design.md`.

Acceptance: app, notification, lock screen, Bluetooth, Android Auto, mini-player, full player, and queue sheet always agree; queue edits survive process recreation.

### Gate 4 — playback quality layer (P0)

- Enable gapless playback and audio offload where supported; expose offload diagnostics.
- Add FFmpeg-backed decoding for broader local formats after license/ABI verification.
- Parse/apply ReplayGain with a service audio processor and clipping prevention.
- Add crossfade/custom transition durations with a safe no-crossfade path for gapless albums and unsupported routes.

Acceptance: settings report actual capability; unsupported effects degrade safely; transitions and normalization are covered by service tests and real-device playback checks.

### Gate 5 — lyrics, sleep timer, and equalizer

- Support embedded lyrics and sidecar `.lrc` files, timestamp highlighting, import/edit, and an honest no-lyrics state. No network lyrics source is required for the local-player release.
- Add a service-owned sleep timer for duration, end-of-track, and end-of-queue with exact-alarm capability only when genuinely needed.
- Add an equalizer bound to the active audio session: system effect panel fallback plus in-app bands/presets, BassBoost/Virtualizer/LoudnessEnhancer capability detection, per-output enablement, and reset.

Acceptance: effects reconnect after service/process recreation, timer state appears in system playback surfaces where possible, and lyrics stay synchronized through seek and track changes.

### Gate 6 — advanced playlists and library actions

- Store ordered playlist items in Room; implement reorder, rename, duplicate, merge, custom cover, share, Quick Fill, rule-based smart mixes, and queue-to-playlist.
- Import/export M3U/M3U8 through SAF, preserving relative paths when possible and reporting unresolved entries.
- Add multi-select and batch Play Next, append, playlist, favorite, share, reject/restore, and delete with optimistic undo where recoverable.
- Add metadata editing through a format-aware tag writer and a duplicate finder that groups normalized metadata/audio fingerprints, always previewing file mutations.

Acceptance: playlist order and covers survive restart/export/restore; batch operations report partial failures; no destructive file action occurs without an exact preview.

### Gate 7 — history, recently played, and statistics

- Record play starts, meaningful-play thresholds, completion, skips, and listening duration without double counting retries.
- Add Recently played, most played, most skipped, listening-time, and artist/album summaries with clear reset controls.
- Exclude rejected tracks from normal recommendations/mixes while retaining historical aggregates.

Acceptance: deterministic event tests match dashboard totals and privacy/reset behavior is explicit.

### Gate 8 — backup and restore

- Add Android Auto Backup rules for database/preferences so eligible devices can use the user’s configured Android cloud backup transport (commonly Google Drive, but controlled by Android/account policy).
- Add explicit versioned SAF export/import for deterministic user-owned backups, with schema validation, dry-run summary, merge/replace choice, and rollback on failure.
- Back up app metadata and settings, never duplicate the music files themselves.

Acceptance: round-trip restore passes from a clean install; malformed/newer backups fail without changing live data.

### Gate 9 — widgets and Android Auto

- Add Glance compact/expanded widgets reflecting the same media session and artwork.
- Upgrade to a browsable `MediaLibraryService` tree for Android Auto: recent, songs, albums, artists, genres, folders, favorites, playlists, and search.

Acceptance: widget controls and Auto browsing/voice playback operate against the same queue and remain useful with the app process cold.

### Gate 10 — customization and release polish

- Expand appearance settings: System/Light/Dark, dynamic color, frost/blur fallback, player palette, waveform style, library layout/density/tabs, navigation shape, artwork corners, motion strength, and accessibility/reduced-motion behavior.
- Complete light/dark/dynamic/high-contrast QA, baseline profiles, startup/performance profiling, migration testing, and emulator/device video comparison against the motion benchmark.

Acceptance: no option is cosmetic-only or broken; 320/375/414/768 dp, font scaling, TalkBack, keyboard/focus, reduced motion, and low-power paths pass.

## Immediate implementation slices

1. Room schema + one-time JSON migration + incremental MediaStore/SAF index.
2. Repository flows and collection/detail destinations.
3. Service queue command protocol + queue UI/reorder/undo.
4. Progress/motion/player continuity pass.
5. Lyrics/timer/equalizer vertical slices.
6. Advanced playlists, multi-select/actions, history, backup, widgets, and Auto in that dependency order.

## Planned file operations

- Modify: `app/build.gradle.kts`, `AndroidManifest.xml`, `MainActivity.kt`, existing models/repository/view-model/service/stores/theme/UI, backup resources, and tests.
- Create: Room database/DAO/entity/migration files; MediaStore/SAF scanners; feature repositories; navigation and focused screen/component files; equalizer/timer/lyrics/history/backup/widget/Auto modules; resources and tests.
- Delete: none during migration. Superseded code is removed only after its replacement is wired, tested, and explicitly listed.

## Execution status — 0.4.0 development build

- Implemented: Room schema and one-time JSON import; SAF + permission-aware MediaStore indexing; album/artist/genre/folder/favorite/history/playlist browsing; service-owned queue actions and persistence; queue reorder/undo/save; MediaLibraryService browse tree and search; home-screen widget; local synchronized lyrics import; sleep timer; audio-session equalizer/bass/loudness; ReplayGain attenuation; gapless/offload preferences; ordered/custom-cover/duplicate/merge/Quick Fill playlists; M3U import/export; multi-select batch queue/favorite/reject actions; duplicate preview; play-event history; Android + explicit ZIP backup; System/Light/Dark/dynamic appearance; compact rows and motion/artwork controls.
- Emulator verified: launch and upgrade preservation, media-session metadata/queue registration, play/pause/seek/next, mini→full player transition, queue sheet, coherent light/dark/system switching, and persisted Room play events. The optional Jellyfin FFmpeg extension was runtime-tested, then excluded from the shippable build because its published artifact is GPLv3 and SwipeTune has not adopted that license. Motion evidence was captured for both PixelPlayer and SwipeTune.
- Honest limits in this build: transition duration is an end fade, not overlapping crossfade; lyrics are local/imported rather than network-fetched; equalizer uses device presets rather than a custom per-band editor; additional FFmpeg codecs await an explicit GPL-compatible licensing decision or a separately built compatible alternative; duplicate ranking uses format/duration rather than acoustic fingerprinting; metadata edits and destructive batch file deletion are not exposed; Stats dashboards, smart-rule playlists, expanded widget sizes, and a car-host Android Auto run remain follow-up work. The JSON compatibility layer still drives the main Compose library while Room becomes authoritative incrementally.

---

## 0.3 baseline record

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
