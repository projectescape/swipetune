# Design — SwipeTune

<!-- Hallmark · pre-emit critique: P5 H4 E4 S5 R4 V4 -->

A locked design system for the Android app. UI work reads this file before implementation. The system follows Material 3 Expressive principles without imitating PixelPlayer's brand or copying its source.

## Product character

- Genre: playful-atmospheric.
- Audience: people listening to and curating a local Android music collection.
- Primary job: play music reliably; review songs quickly when the user enters Review mode.
- Voice: warm, concise, lightly mischievous. Never cute at the cost of clarity.

## App structure

The app uses a listening-workbench structure:

- Library is the default destination and organizes Songs, Albums, Artists, Folders, and Favorites.
- Review is a purpose-built swipe flow with its own playback queue and undo history.
- Settings owns appearance, library location and maintenance, playback preferences, and app information.
- A persistent mini-player floats above navigation whenever a track is loaded.
- Tapping the mini-player opens one immersive full player shared by every destination.

Search, sort, queue, and collection detail views are contextual layers rather than permanent top-level tabs. Rejected review items never appear in the normal library unless restored.

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

- Frost is reserved for the floating mini-player, bottom navigation, queue sheet, and transient overlays.
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

- Navigation and player expansion use shared-axis or container-transform-like motion.
- Playback controls use short spring responses; play/pause may morph or crossfade.
- Review cards track the finger, rotate subtly, and reveal Keep/Reject intent progressively.
- Undo restores the prior card and playback state with a reversible spring.
- List changes animate placement, not every property.
- Respect system animator scale and reduced-motion expectations. No ambient looping animation.

## Controls and states

- Every action has enabled, pressed, focused, disabled, loading, error, and success behavior where applicable.
- Playing rows expose a visual indicator in addition to color.
- Seek controls show elapsed and remaining/total time.
- Shuffle, repeat, and favorite controls expose selected semantics for accessibility.
- Empty, scanning, permission-loss, missing-file, and playback-error states receive explicit copy and recovery actions.

## Responsive behavior

- Verify compact widths equivalent to 320, 375, and 414 dp and an expanded 768 dp layout.
- Compact layouts use bottom navigation; expanded layouts may use a navigation rail and two-pane library/player composition.
- No horizontal scrolling except intentional filter/tab rails.
- Full-player artwork is constrained by available height so controls never fall below system insets.

## What every screen shares

- Semantic Material color roles and the same fallback palette.
- The mini-player, player controls, artwork treatment, shapes, spacing, and motion timing.
- Edge-to-edge system bars with icon contrast derived from the active surface.
- Real metadata or honest empty states; no fabricated content or dead controls.

## Scope boundary

The first complete baseline includes service-owned playback, global Android media controls, library indexing and browsing, favorites, search/sort, queue controls, full player, swipe review, appearance/settings, and robust empty/error states. Lyrics, equalizer, widgets, listening statistics, tag editing, cloud sources, and Android Auto are subsequent modules and must not appear as inert UI before implementation.

