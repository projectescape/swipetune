# Design — SwipeTune

<!-- Hallmark · pre-emit critique: P5 H5 E5 S5 R5 V5 -->

A locked design system for the Android app. UI work reads this file before implementation. The system follows Material 3 Expressive principles without imitating PixelPlayer's brand or copying its source.

## Product character

- Genre: playful-atmospheric.
- Audience: people listening to and curating a local Android music collection.
- Primary job: play the complete device library reliably. Review is an optional curation tool entered deliberately from Settings.
- Voice: warm, concise, lightly mischievous. Never cute at the cost of clarity.

## App structure

The app uses a listening-workbench structure:

- Library is the default destination and automatically indexes device audio through MediaStore; user-picked folders are optional additional roots.
- Library is the only primary surface. Settings is a top-right icon action, with no bottom navigation or repeated app wordmark. Review is not a peer destination or part of the brand promise.
- Settings owns appearance, library sources and maintenance, playback preferences, Review launch/scope/history, and app information.
- Review is a purpose-built, explicitly launched flow with its own playback queue and undo history. It can target all unreviewed music or one indexed folder.
- A persistent mini-player floats above the system navigation inset whenever a track is loaded.
- Tapping the mini-player opens one immersive full player shared by every destination.

Search, sort, queue, collection details, Review, and review-decision management are contextual layers rather than permanent top-level tabs. Accepted and rejected decisions persist across app and Review restarts. Rejected tracks never appear in the normal library unless their decision is cleared; accepted tracks remain available normally.

Accepted and Rejected also appear as built-in smart playlists. They update directly from Review metadata and cannot be renamed or deleted. Rejected is the sole deliberate library exception where hidden tracks can be inspected, played, or restored.

## Visual hierarchy

1. Current album artwork is the strongest visual anchor.
2. Track title and immediate playback action are next.
3. Collection browsing and review decisions follow.
4. Secondary metadata and utilities remain quiet.

The app must remain legible when artwork is missing, nearly black, or extremely bright. Artwork-derived atmosphere is decorative; semantic theme colors always own text and controls.

## Color

- Prefer Android dynamic color on Android 12+ when the user enables it.
- Provide System, Light, and Dark theme modes independently of dynamic color.
- Fallback light palette: warm mist background, ink-plum text, violet primary, coral secondary.
- Fallback dark palette: near-black aubergine background, pale lavender text, luminous violet primary, warm coral secondary.
- Rejected/destructive actions use the Material error role; kept/favorite actions use tertiary/primary roles.
- Never place text directly on untreated artwork. Use tonal scrims or opaque semantic surfaces.

## Surfaces and frost

- Frost is reserved for the floating mini-player, queue sheet, and transient overlays.
- Library rows and settings use quiet tonal surfaces, not nested glass cards.
- Frost combines a translucent semantic surface, a subtle outline, and backdrop color sampling where the platform allows it. Blur is never required for contrast.
- Shape language is rounded and slightly asymmetric: 28–36 dp hero surfaces, 18–24 dp rows/sheets, pills only for compact filters and state selectors.

## Typography

- Use the Android system font stack for native rendering and accessibility.
- Display: Material expressive headline styles, 600–700 weight, upright.
- Body: Material body styles, 400–500 weight.
- Labels: 500–600 weight; sentence case.
- Track titles may use one strong line in lists and two lines in the full player. Artist/album text remains subordinate.

## Spacing

Use a 4 dp grid. Standard spacing values are 4, 8, 12, 16, 20, 24, 32, and 40 dp. Interactive targets are at least 48 dp. Edge padding is 16 dp on compact phones, 24 dp on medium layouts, and 32 dp on expanded layouts.

## Motion

- Navigation uses a directional shared-axis transition: outgoing content recedes and fades while incoming content arrives from the navigation edge. Persistent playback chrome never disappears during the transition.
- Mini-player arrival and player expansion preserve artwork, title, and transport continuity. Expansion behaves like a container transformation into the full player rather than a route replacement.
- Playback controls use short, critically damped spring responses. Play/pause morphs in place; selected shuffle, repeat, and favorite controls change both fill and shape without shifting neighboring controls.
- Playback must always look alive: the mini-player progress line advances continuously, the active library row has a small equalizer indicator, and the full-player waveform/progress head moves while audio plays. Functional progress continues under reduced motion.
- Seek gestures update the waveform and elapsed time synchronously with the finger, then settle without overshoot. Large seek jumps use a brief tonal pulse rather than a toast.
- Queue and settings surfaces rise as anchored sheets with a visible drag handle, dimming the source without destroying its visual context. Queue reordering lifts only the dragged row and animates surrounding placement.
- Press feedback starts on touch-down (tonal compression plus a 0.98 scale where space permits), completes within roughly 120 ms, and never delays the resulting action.
- Major transitions target 320–420 ms; ordinary sheets and destination changes 220–320 ms; control feedback 90–150 ms. Entrances decelerate, exits are shorter, and no control bounces.
- Review cards track the finger, rotate subtly, and reveal Keep/Reject intent progressively.
- Undo restores the prior card and playback state with a reversible spring.
- List changes animate placement, not every property.
- Respect system animator scale and reduced-motion expectations. Spatial motion collapses to a short crossfade, while progress, buffering, and playback-state indicators remain functional. No decorative ambient looping animation.

### PixelPlayer motion study, 2026-10-25

The reference build was exercised on the same Pixel 9 API 36 emulator with real copied MP3 files and captured as video. Transferable behavior—not branding, layout, assets, or source—includes:

- continuity between library row, mini-player, full player, system session, and queue;
- whole-surface selected states instead of color-only icons;
- a waveform that carries both progress and direct manipulation;
- asymmetric expressive containers that morph without moving labels unexpectedly;
- bottom sheets that retain the source screen as spatial context;
- subtle active-track motion and immediate touch-down response;
- drag reordering where neighboring rows smoothly make room.

SwipeTune should match that perceived responsiveness while retaining its own frosted, artwork-led Material 3 Expressive identity. Review remains a powerful secondary tool rather than defining the main player.

## Controls and states

- Every action has enabled, pressed, focused, disabled, loading, error, and success behavior where applicable.
- Playing rows expose a visual indicator in addition to color.
- Seek controls show elapsed and remaining/total time.
- Shuffle, repeat, and favorite controls expose selected semantics for accessibility.
- Empty, scanning, permission-loss, missing-file, and playback-error states receive explicit copy and recovery actions.

## Responsive behavior

- Verify compact widths equivalent to 320, 375, and 414 dp and an expanded 768 dp layout.
- Compact layouts keep Settings in the top app bar; expanded layouts may use a two-pane library/player composition without introducing a one-item navigation rail.
- No horizontal scrolling except intentional filter/tab rails.
- Full-player artwork is constrained by available height so controls never fall below system insets.

## What every screen shares

- Semantic Material color roles and the same fallback palette.
- The mini-player, player controls, artwork treatment, shapes, spacing, and motion timing.
- Edge-to-edge system bars with icon contrast derived from the active surface.
- Real metadata or honest empty states; no fabricated content or dead controls.

## Scope boundary

The complete local-player target includes service-owned playback and Android media controls; a Room-backed MediaStore/SAF library; detailed collections; advanced queue and playlists; Review mode; lyrics, sleep timer, equalizer, ReplayGain and transition controls; batch library actions; history and statistics; explicit export/import plus Android cloud backup; widgets; Android Auto; and deep appearance/motion customization. Remote streaming services remain out of scope. No module appears as inert UI before its working vertical slice is wired.

