# SwipeTune interaction audit

Audited on 2026-10-26 against a physical Android phone with 1,799 indexed songs.

## Interaction contract

- Tap feedback begins immediately through Material ripple, tonal state, shape change, or a visible loading state.
- Navigation within SwipeTune uses a 180–360 ms fade plus restrained shared-axis movement. Back reverses the spatial direction where a parent/child relationship exists.
- Continuous gestures track the finger. Seek time and the wavy progress head update during drag; Review cards expose intent during drag; library pages follow horizontal swipes.
- Android-owned permissions and document/folder pickers keep their platform transitions.
- Large work never blocks first feedback. Playback starts from a small queue window and hydrates the remaining queue in chunks; library refresh keeps cached content visible; JSON persistence runs off the main thread.

## Onboarding

| Action | Feedback / transition |
| --- | --- |
| Choose music folder | Material press feedback, then Android's folder picker transition |

## Library

| Action | Feedback / transition |
| --- | --- |
| Search | Results update continuously without moving the search field |
| Change sort | Material press feedback; list content updates in place |
| Shuffle library | Immediate selected-track state, then playback |
| Open Settings | App-level fade/scale transition; Back returns through the same layer |
| Tap a section chip | Pager animates to the destination and the selected chip changes tone |
| Swipe Songs / Albums / Artists / Genres / Folders / Favorites / History / Playlists | Content tracks the finger, settles to the matching chip, and keeps that chip visible in the horizontal rail |
| Tap a song | Row changes color and shape immediately; audio starts before the full queue is hydrated |
| Long-press a song | Selection actions appear with animated visibility |
| Favorite / overflow / queue / playlist actions | Native control feedback; menus and dialogs use Material transitions |
| Open album, artist, genre, or folder | Forward shared-axis detail transition |
| Back from collection detail | Reverse shared-axis transition; both toolbar and system Back are supported |

## Playlists

| Action | Feedback / transition |
| --- | --- |
| Open built-in or user playlist | Forward shared-axis detail transition |
| Back to playlists | Reverse shared-axis transition; toolbar and system Back are supported |
| Create, delete, duplicate, fill, merge, reorder, remove | Material feedback; destructive actions use confirmation dialogs |
| Import / export / choose cover | Android document picker transition |
| Play playlist or playlist song | Immediate playback state; queue fills asynchronously |

## Mini-player and Now Playing

| Action | Feedback / transition |
| --- | --- |
| Tap mini-player body | Player expands with fade, vertical travel, and subtle scale continuity |
| Drag or tap the mini-player progress strip | Wavy progress and its thumb follow the finger and seek without opening Now Playing |
| Close with chevron or system Back | Player contracts through the reverse exit transition |
| Play / pause | Icon morph and expressive shape change |
| Previous / next | Immediate transport feedback and metadata/artwork update |
| Seek | 48 dp direct-manipulation target; elapsed time and wavy head follow the finger; the wave travels during playback, flattens under touch, the thumb becomes a grip, and release seeks once |
| Favorite / shuffle / repeat | Material feedback plus persistent selected color/state semantics |
| Open Queue or Lyrics | In-player shared-axis transition |
| Back from Queue or Lyrics | Returns to Now Playing before dismissing the player |

## Queue and lyrics

| Action | Feedback / transition |
| --- | --- |
| Select queue item | Immediate transport state and playback |
| Reorder, remove, clear, undo | Material feedback; affected rows animate into their new placement |
| Save queue | Material dialog transition |
| Select timestamped lyric | Immediate seek and active-line state |
| Import lyrics | Android document picker transition |

## Review

| Action | Feedback / transition |
| --- | --- |
| Drag card | Card follows the finger, subtly rotates, and progressively reveals Keep/Reject intent |
| Keep / Reject | Expressive button feedback and reversible next-card transition |
| Undo | Restores prior decision and playback state |
| Seek / skip 10 seconds / play-pause | Same direct playback feedback as Now Playing |
| Open Results / close Review | App-level navigation transition; system Back is supported |

## Review decisions

| Action | Feedback / transition |
| --- | --- |
| Switch Accepted / Rejected | Directional fade/slide content transition |
| Clear or restore decision | Material feedback and immediate list update |
| Delete rejected files | Explicit destructive confirmation and visible loading state |
| Back | App-level return transition through toolbar or system Back |

## Settings

| Action | Feedback / transition |
| --- | --- |
| Theme, dynamic color, appearance, and playback switches | Native animated Material selection states |
| Equalizer and quality controls | Sliders track continuously; chips and switches retain selected state |
| Rescan library | Cached library remains usable while indexing continues in the background |
| Add folder, backup, restore | Android picker transition |
| Start / continue Review and open decisions | App-level destination transition |
| Find duplicates, sleep timer, export | Immediate Material feedback and visible result/status |
| Back | App-level return transition through toolbar or system Back |

## Performance findings resolved

- Full-library `MediaItem` construction and submission previously happened synchronously before playback. The selected track and a 32-item forward window now start first; the remaining queue is added in 128-item chunks after playback has had time to settle.
- Playback queue serialization previously ran on the media-service main thread. Serialization now runs on an IO coroutine after a short debounce.
- App-state JSON previously serialized the entire library synchronously for every setting, playlist, favorite, or Review decision. Saves are now conflated and written on IO.
- A cached library was previously replaced by a blocking loading screen during each refresh. Loading now covers the UI only when no cached songs exist.
- Activity recreation and foreground return previously announced a new device scan. Foreground catch-up now uses a cooldown and remains silent when cached music exists; only first indexing and an explicit Rescan action announce progress.
- Review queue reconciliation previously performed a nested linear search. It now uses a URI set.
- Opening a large playback queue previously resolved every URI with a nested library search. It now builds one URI map and renders immediately through the lazy list.
