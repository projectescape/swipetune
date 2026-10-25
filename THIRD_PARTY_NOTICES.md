# Third-party notices

SwipeTune is distributed under the GNU General Public License v3.0.

## Jellyfin AndroidX Media FFmpeg decoder

SwipeTune includes `org.jellyfin.media3:media3-ffmpeg-decoder:1.9.0+1`, published by the Jellyfin project under GPLv3.

- Source: https://github.com/jellyfin/jellyfin-androidx-media
- Releases: https://github.com/jellyfin/jellyfin-androidx-media/releases
- License declared by the published artifact: GNU General Public License v3.0

## FFmpeg

The decoder contains FFmpeg. FFmpeg is primarily LGPL 2.1-or-later, while builds using optional GPL components are governed by GPL 2-or-later. SwipeTune distributes the combined application under GPLv3.

- Source and license information: https://ffmpeg.org/
- Legal information: https://ffmpeg.org/legal.html

The exact dependency versions needed to reproduce SwipeTune are declared in `app/build.gradle.kts` and resolved by Gradle.
