# Home KTV

[中文](README.md) | **English**

Home KTV turns a NAS or Linux host into a private, LAN-only karaoke system:
use the Android TV app for playback, scan the on-screen QR code with a phone to
pick songs, and manage the music library from a browser. The server keeps the
queue, lyrics, playback state, media processing, and song metadata in sync in
real time.

> Home KTV is designed for a trusted home LAN. It does not provide public-login
> or Internet-facing security controls. Do not expose it directly to the
> Internet.

## Screenshots

| Mobile songbook | TV playback | Library and service dashboard |
| --- | --- | --- |
| ![Mobile songbook](docs/images/mobile-songbook.png) | ![TV playback](docs/images/tv-player.png) | ![Admin dashboard](docs/images/admin-dashboard.png) |
| Browse, search, favorite, and queue songs from a phone. | Big-screen playback, animated lyrics, and QR-code song selection. | Track the library, transcoding work, and playback service from one place. |

## Highlights

- **Three connected clients:** Spring Boot server, Vue 3 mobile songbook and
  administration UI, plus an Android TV player based on Media3/ExoPlayer.
- **Phone-first song selection:** no app installation or registration required;
  search by title, artist, Chinese name, full pinyin, or pinyin initials.
- **TV playback built for karaoke:** dual audio tracks, vocal/accompaniment
  switching without reloading, animated LRC lyrics, remote-control support,
  reconnect recovery, QR-code pairing, and burn-in protection.
- **Media-library workflow:** scan source media, inspect with FFprobe, dedupe by
  MD5, direct-copy compatible files, transcode incompatible media, and import
  sidecar lyrics and cover art.
- **Real-time room control:** the queue, playback state, lyrics, volume, and
  controls synchronize through WebSocket.

## Quick Start

### Requirements

- A NAS, Linux host, or Docker Desktop installation with Docker Compose
- At least 1 GB of available memory is recommended
- Phone, Android TV, and server on the same LAN
- Android TV 10 or later

### 1. Configure storage and credentials

```bash
git clone <repository-url>
cd home-ktv
cp .env.example .env
```

Set separate host directories for source media and the playable library, then
replace the database password:

```dotenv
KTV_SOURCE_MUSIC_DIR=/volume1/home-ktv/source-music
KTV_MUSIC_DIR=/volume1/home-ktv/music
KTV_DB_PASSWORD=replace-with-a-strong-password
```

The server writes processed files into `KTV_MUSIC_DIR`, so ensure the container
has write access. Never point both directory variables at the same path.

### 2. Start the stack

The recommended deployment pulls the multi-architecture image published by
GitHub Actions and does not compile anything on the NAS or host:

```bash
docker compose -f docker-compose.prebuilt.yml up -d --pull always --wait
```

It uses `ghcr.io/zhayinggang/ktv-home:latest` by default. For production, set
`KTV_RELEASE_IMAGE` in `.env` to a specific release tag so upgrades are
explicit.

To build from source instead, run:

```bash
docker compose up -d --build --wait
docker compose ps
curl http://127.0.0.1:${KTV_HTTP_PORT:-8080}/api/health
```

| Service | Default port | Purpose |
| --- | --- | --- |
| TCP | `8080` | Mobile UI, administration UI, API, WebSocket, and media streaming |
| UDP | `18888` | Android TV LAN discovery |

Allow both ports through the NAS or host firewall. The TV app scans the exact
UDP discovery port configured by `KTV_DISCOVERY_UDP_PORT`; when changing it,
update the TV scan configuration at the same time. Use the values in `.env` if
you override the defaults.

### 3. Add songs and connect the TV

1. Copy source media to `KTV_SOURCE_MUSIC_DIR`.
2. Open `http://<host-ip>:8080/m/admin` and choose **Scan source path**.
3. Review files in **Source Library** and start transcoding where needed.
4. Install the Android TV APK, then let it discover the server or enter
   `<host-ip>:8080` manually.
5. Scan the QR code shown on TV and start picking songs at
   `http://<host-ip>:8080/m`.

Build a debug TV APK with JDK 17 and the Android SDK:

```bash
cd android-tv
./gradlew testDebugUnitTest assembleDebug
```

The resulting APK is at
`android-tv/app/build/outputs/apk/debug/app-debug.apk`.

## Common URLs

| Purpose | URL |
| --- | --- |
| Mobile songbook | `http://<host-ip>:8080/m` |
| Administration | `http://<host-ip>:8080/m/admin` |
| Health check | `http://<host-ip>:8080/api/health` |

## Local Development

Development requires Node.js 20+, JDK 21, JDK 17, Docker, and the Android SDK.

```bash
# PostgreSQL
docker compose -f docker-compose.dev.yml up -d

# Backend (JDK 21)
cd backend && ./mvnw spring-boot:run

# Mobile web app
cd h5 && npm install && npm run dev

# Android TV (JDK 17)
cd android-tv && ./gradlew testDebugUnitTest assembleDebug
```

Run tests with `cd backend && ./mvnw test`, `cd h5 && npm test`, and
`cd android-tv && ./gradlew testDebugUnitTest`.

## Media Notes

Home KTV prioritizes embedded media metadata and falls back to filenames such
as `Artist - Title.mp4`. Put same-named `.lrc` files beside media files for
line-by-line or enhanced word-timed lyrics. For dual-track karaoke videos, use
the order **vocal first, accompaniment second** and preferably label the tracks
`vocal` / `accompaniment` (or `原唱` / `伴奏`).

## Hardware Transcoding

CPU transcoding is the default. Linux hosts with Intel or AMD VAAPI devices can
add `docker-compose.hardware.yml`; Rockchip hosts can add
`docker-compose.rockchip.yml`. See the [Chinese hardware-transcoding section](README.md#硬件转码)
for the exact commands and host-device requirements.

## License and Media Responsibility

The code is released under the [MIT License](LICENSE). Only import and play
media that you are authorized to use. Home KTV neither provides nor distributes
music, videos, or accompaniment tracks.
