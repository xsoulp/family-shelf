# Family Shelf — project history

Archived development notes, translated into English. Entries describe the version
available at the time; older layouts, app shortcuts, asset paths and instructions
may have been superseded. Use the [README](README.md) for current setup and
[validation records](VALIDATION.md) for test results. Historical artifact filenames
and exact external library/filename matching keys are preserved. Personal names
and local addresses have been replaced with generic examples.

## Silent, smaller aircraft over video — version 0.15.3

Aircraft over Netflix/Jellyfin now render at half their previous width and height,
with readable callsign/airline text. Removed the video overlay's sound player and
flyby cue, so streaming aircraft remain silent regardless of the music setting.
Screensaver aircraft keep their existing size and pass-by sound.

## Creator credit and public APK workflow — version 0.15.2

Added a persistent © xsoulp 2026 credit to room/screensaver and Jellyfin connection
windows, every app dialog, the aircraft overlay and the browser design preview.
The credit does not receive focus, intercept touch or pan with the bookcase.

The public README is organized around features, downloading/installing an APK,
compiling and personalizing. Advanced notes moved to docs/REFERENCE.md. A clean
GitHub Actions build publishes ordinary and Home-enabled APK artifacts, excludes
private assets and uses a dedicated public development key for consistent updates.

## Realistic manga airliner — version 0.15.1

A generated transparent airliner sprite replaces the simple vector aircraft in
both scenes. Painted fuselage shading, panel/window detail, swept wings and the
turbofan give it more realistic manga volume. Native tinting is clipped to the
registered tail and accent stripe so existing operator colours remain dynamic.
Only the callsign and known airline appear above; there is no lower label.

The audio-release settling timestamp is shared across launcher Activity instances,
so switching between MainActivity and the Home alias does not mistake the previous
instance's just-released music for another app's playback. The guard against
interrupting other apps remains in place.

## Hourly church bell — version 0.15.0

Downloaded a real single church-bell strike from BigSoundBank under CC0. While the
outdoor screensaver is visible and audible, an observed local-hour boundary
starts 1–12 strikes at quiet gain, with twelve at noon/midnight. The configured
scene zone matches the tower clock; device time is the fallback. Original decays
overlap between strikes. Settings provide a toggle and one-strike preview.

Mute, focus loss, leaving the screensaver or screen sleep cancels pending strikes. Launch,
wake and clock changes do not replay missed hours; a saved boundary marker avoids
Activity duplicate chimes. No background alarm or wake lock is added.

## Single aircraft caption — included in version 0.15.0

Removed the lower data-credit caption from video aircraft. Only the callsign and
known airline remain above each plane. Provider and ODbL URLs are available in the
video-aircraft About dialog and third-party notices; the screensaver footer stays.

## Painted manga Kids room and integrated app toys — version 0.14.0

Repainted the full continuous six-shelf room with manga ink lines, warm cel shading
and a hand-painted finish. Shelf interiors stay empty for live covers and retain
their existing registration. Three tennis balls and their contact shadows now
belong to the room image itself, with blocks to the left and a train to the right.
Native ball sprites are removed. Transparent clickable app targets align with the
painted balls; only the selected ball's outline is drawn live. Left-to-right app
order stays Jellyfin, Red Descent, Netflix. Labels remain absent, and perspective
cover selection, vertical camera motion and room-edge navigation are retained.

## Realistic floor tennis balls — version 0.13.1

Generated a transparent photographic felt tennis-ball texture with recessed ivory
seams and warm directional lighting. All three app toys use the shared decoded
bitmap with soft native contact shadows and existing focus outlines. No printed
labels or logos are added. The compact 420-unit foreground cover and the painted
clouds from the preceding release remain included.

## Perspective books and painted clouds — version 0.13.0

Shelf covers have a projective book pose. Focus pulls the selected cover forward
and straightens it into a large centred poster, while the previous cover returns
over 420 ms. A soft room dim and shadow emphasize depth; remote navigation and
foreground-cover touch activation keep their existing actions. Both rooms use the
same geometry, with Kids camera offset respected on every shelf.

A generated RGBA cloud sprite replaces the flat procedural cloud silhouettes.
Soft anime-style painted lighting adds natural volume and wispy edges; layered
sizes, mirroring and day/night/overcast tinting reuse it with the existing weather
coverage and wind motion. The image prompt is saved with the asset documentation.

## Unlabelled Kids room — version 0.12.4

Removed the Kids room HUD, section badges, shelf counter, navigation-hint bar,
room/options text controls and app names printed on the floor balls. Real cover
artwork, the decorative Family Shelf sign, progress and selection outlines stay.
Menu and long-press OK retain settings access; room edges and vertical navigation
continue working. Accessibility descriptions remain available. Removed invisible
section tap targets along with their labels. Adults visuals are unchanged.

## Aircraft engine pass-by sound — version 0.12.3

Replaced the generic whoosh with Airplane, pass #4 by Dj Lio/BigSoundBank (CC0):
an engine/propeller pass with Doppler movement. The downloaded sound retains its
full duration, converted to mono 22,050 Hz PCM16 to fit SoundPool sample memory.
The shared asset plays outdoors and over Netflix/Jellyfin at the existing quiet
gains, with unchanged once-per-aircraft timing and mute behaviour. No audio was
generated. Source and hashes are recorded in the provenance file.

## Jellyfin missing playback-state compatibility — version 0.12.2

Device playback exposed a Jellyfin Android TV client that reports STATE_NONE
through Android MediaSession while its server reports advancing video playback.
A read-only session fallback now verifies recent unpaused video on the same
account, local network address and client. It polls only for a qualifying Jellyfin
media session with missing Android state. Normal PLAYING/PAUSED states retain the
native path; failed, stale, other-device/account or audio-only records fail closed.
Existing permissions, focus behaviour and real aircraft detection are preserved.

## Jellyfin video aircraft — version 0.12.1

The existing live aircraft overlay now supports Jellyfin Android TV, Jellyfin
mobile and Findroid as well as Netflix. Only active playback in a supported app
qualifies. A known foreground package must match the playing session, preventing
stale background playback in the other app from enabling an overlay.

Options now calls the shared feature Video aircraft overlay. The service component
and saved preference retain their existing internal identifiers to preserve the
user's permissions and on/off selection. Aircraft artwork, bridge detection,
whoosh, focus/input behaviour and screensaver-only music remain unchanged.

## Netflix aircraft and manga styling — version 0.12.0

The existing real bridge-crossing detector can run while Netflix reports active
video playback. A system-bound playback observer adds a temporary transparent,
non-interactive aircraft window and a quiet whoosh, without starting outdoor music
or controlling the player. Android overlay and playback-observation permissions
are configurable through Options. Paused playback, screen-off and launcher return
stop the window. Notification bodies and screen contents are not inspected.

A shared native Canvas renderer gives both scenes ink outlines, cel shading and
speed lines. Known easyJet, Ryanair, Vueling and TAP callsigns select illustrative
operator tail colours; unknown operators use neutral colours. The documented
example sheet is a native off-screen rendering labelled as design examples,
separate from real flight detections.

## Quiet outdoor music and aircraft whoosh — version 0.11.1

The private outdoor screen loops the supplied Bonito, Bonito MP3 at 7% gain, with
a short fade-in. Music uses the same audio-focus lease across room/outdoor track
changes. The existing mute, pause and focus-loss behaviour also applies outdoors;
user audio is kept only in ignored local assets and private APKs.

Each aircraft animation triggers one quiet CC0 Air whoosh recording by
PyranoStudios/OpenGameArt as it approaches the centre of the sky. The downloaded WAV
is unchanged and plays once at 16% gain. A per-plane cue prevents repeat playback
from frames or API polling and cancels when the scene is hidden.

## Lisbon outdoor screensaver — version 0.11.0

A foreground-only screensaver opens after two minutes without remote input. Matching
manga-style day/night Lisbon plates show the castle viewpoint, rooftops, Tagus and
Ponte 25 de Abril. Native overlays animate clouds and weather effects; the church
bell-tower clock displays the real local time. Open-Meteo supplies current model
conditions and solar times, with source timestamps and explicit stale/unavailable
readings. Private location settings and the original Adults-room photo stay local.

Fresh ADSB.lol aircraft positions can trigger a small animated plane when they
enter or cross the approximate bridge corridor. The free community API is polled
only while visible, with stale/ground filtering, duplicate suppression, short-gap
segment checks, backoff and Retry-After handling. Coverage limitations can miss a
pass; random aircraft are never used. Provider/ODbL credit is shown on screen.

The wake input is consumed, room music pauses, and pausing/sleeping stops the scene.
Options provide an on/off switch and explicitly labelled day/night previews.

## Private Adults-room photograph — version 0.10.4

An optional local JPEG can replace the fictional photograph inside the existing
Adults-room frame. The renderer draws the original image with a centered crop;
faces are not regenerated. Personal assets are read from an ignored private asset
directory, with an explicit Gradle directory override available. The public room
background and example screenshots retain their fictional portraits.

## Tennis-ball app shortcuts — version 0.10.3

Kids floor shortcuts now look like tennis balls with cream curved seams and the
app name printed as a brand directly on the felt. The installed app icons and
external labels are removed from these balls. Their shading is fully opaque;
selection, accessibility labels, launching and room-edge navigation remain in place.

## Startup poster recovery — version 0.10.2

The launcher previously fetched shelves once on resume and discarded failed poster
downloads. A network that was still connecting at boot could therefore leave empty
positions until another app was opened and the launcher resumed.

Temporary metadata and artwork failures now retry with bounded backoff while the
launcher is visible. Poster work has a separate three-thread executor so artwork
cannot block metadata refreshes. A bounded, app-private disk cache restores saved
shelves and Jellyfin covers across process restarts; cache scope includes the account,
room, library choices and Netflix selection. Cached shelves remain visible when a
refresh fails temporarily. Sign-out clears the startup cache, and rejected sessions
or missing artwork do not enter automatic retry loops. Screen-on refreshes are added.

## Room edge navigation and floor app toys — version 0.10.1

Moving beyond either the left or right end of any Kids or Adults shelf now
switches to the other room. Entry uses the opposite edge and corresponding
upper/lower row. The last Kids section is retained, so a reverse crossing returns
to the same boundary cover, including the six-column Disney shelves. App-row
boundaries switch rooms in the same way and preserve app focus.

Kids app shortcuts now sit on the room's floor as native Canvas-drawn toy balls,
with the installed Jellyfin/Netflix/game artwork, shadows, stitched panels and a
selection ring. Back or Down after the last shelf pans to the floor. The balls
move with the scene's camera while Options and navigation hints stay fixed.

## Continuous Kids bookcase and room music — version 0.10.0

The approved preview is implemented as one tall Kids bookcase: two story shelves,
a row of ongoing Kids media, recent Kids additions, and two six-cover Disney rows.
Up/Down pans the camera vertically within that scene, preserving the nearest
column; horizontal shelf edges no longer change Kids rooms. Adults retains its
existing scene. A room control and Options entry switch between Kids and Adults.
Old Books/Kids/Disney room IDs migrate to their corresponding bookcase positions.

Two free-to-use downloaded recordings by Kevin MacLeod are bundled locally:
Music Box Theme for Kids and Darkest Child for Adults, under CC BY 4.0 with
attribution. Original source MP3 files are included unchanged. Background music
has a saved on/off preference and responds to lifecycle, screen state and Android
audio focus. Sources, license and author credits are documented in the project
and in the launcher's Help / controls dialog.

## Jellyfin server compatibility and distinct recent movies — version 0.9.10

Authentication now includes the saved session token in the standard MediaBrowser
Authorization header. Library discovery uses `UserViews?userId=…`, and ongoing
shelves use `UserItems/Resume` with user, parent-library and item-type filters.
Legacy user-specific routes remain a fallback only for HTTP 404. Session failures
and permission errors are reported separately instead of a generic refresh error.

Recent movie selection excludes all ongoing Jellyfin movie IDs returned for the
view and the confirmed Netflix movie displayed there. Identity matching accepts
compact and hyphenated Jellyfin UUIDs; different Jellyfin films sharing a title
remain distinct. The selector continues through date-ordered recent candidates
to fill two places, using an explicit empty position when no alternative exists.
Netflix title matching is used only across providers, which do not share item IDs.

## English interface and documentation — version 0.9.9

Menus, messages, accessibility descriptions, room signs and example interface
labels now use English. The app and accessibility service use the Family Shelf
name. The Disney catalogue displays English titles while retaining Portuguese
and original-title aliases to match existing Jellyfin metadata. Books & Stories
accepts English library names and the existing `Livros e historias` name.
Historical prose is translated; technical paths and observed media titles remain
unchanged. The update is prepared locally and in GitHub without deployment to TV.

## Red Descent in the Kids view — version 0.9.8

The central Kids shortcut opens **Red Descent** (`pt.reddescent.game`) using its
installed app icon. It replaces Lado a Lado at the former SmartTube position and
covers the SmartTube emblem baked into the room image. Adults and the remote's
global YouTube button continue to use SmartTube.

---

## Lado a Lado in the Kids view — version 0.9.7

The Kids room's central shortcut uses the installed **Lado a Lado** game icon
(`com.xsoulp.sidebyside`) and opens that game. The icon covers the SmartTube emblem
without changing the rest of the background. Switching to Adults restores
SmartTube in the same position. The remote's global YouTube mapping remains
SmartTube, as configured in the preceding version.

---

## Remote buttons — version 0.9.6

On the development Xiaomi remote, Netflix (`BUTTON_6`) opens Netflix, Amazon /
Prime Video (`BUTTON_7`) opens Jellyfin, and YouTube (`BUTTON_3`) opens SmartTube.
The launcher handles these keys with dialogs or rooms without apps open. For
cross-app operation, enable the remote-button accessibility service through
**Options → Remote buttons: Netflix / Jellyfin / SmartTube**. Its former label
was **Living Room remote buttons**. The service filters only these three keys
and cannot read screen contents. Disabling it removes the global remapping.
No system key-layout file changes are required.

The codes come from `/vendor/usr/keylayout/Vendor_2717_Product_32b9.kl`: Netflix
uses scan codes 240/416, Prime Video uses 207, and YouTube uses 425. Other remotes
and devices may use different codes.

---

## New Disney Library background — version 0.9.4

The Disney room uses the user-supplied `fundo_disneyteca.png`, copied unchanged to
`app/src/main/assets/library-disneyteca.png`. Twelve posters occupy two shelves of
six, with 180×280 scene-unit covers aligned to the sloping upper shelf. Film order
and navigation are unchanged. Background provenance and coordinates are recorded
in the accompanying `.provenance.json` file.

---

## Netflix view separation and fallback — version 0.9.3

Each Netflix position uses, in order, the confirmed title in this view's Watch Next
feed, its last saved record for this view, or the second ongoing Jellyfin movie or
series from the same view. If Jellyfin also has too few titles, the position keeps
access to that view's libraries. The first Jellyfin item is not duplicated. Saved
records preserve progress and the original link; their description identifies
when they were saved. A real refreshed Netflix record supersedes the cache.

Confirmed Kids associations are Goat / The Loud House Movie / The Loud House;
Adults associations are Eat Pray Bark / Everwood / Running Point. Goat becomes
the initial Kids movie after user confirmation. Titles assigned to the other view
are hidden from the Netflix selector. New associations require an explicit choice
in **Options → Netflix for this view**. Association is by title because Netflix
does not expose profiles; progress from two profiles watching the same title
cannot be distinguished.

---

## Netflix cover artwork — version 0.9.2

Netflix covers use portrait posters from the public Cinemeta API, with a local
cache of up to 24 images. Only exact title/type matches are accepted; ambiguous
results retain Netflix's own image. Running Point uses confirmed IMDb identity
`tt14879018` to identify the 2025 series. Covers fit within the books without
cropping titles or faces. Progress and playback links still come exclusively from
Netflix Watch Next. Artwork credits appear under **Options → Help / controls**.

---

## Netflix selection per view

Use **Options → Netflix for this view** to choose the ongoing movie and series
shared by the device. Adults titles are chosen from the adult profile. Selections
are saved separately from Kids and replace the earlier fixed defaults. Without
an accessible Netflix record or cache, the position uses Jellyfin. Watch Next does
not identify Netflix profiles, so the user must confirm the association. If a title
is absent, open that profile in Netflix and return to refresh; availability depends
on what Netflix shares.

---

## Disney catalogue matching — version 0.9.1

Snow White and the Seven Dwarfs accepts 1937 or 1938 for the same animation.
The exact library filename `A Bela Adormecida.ia` was confirmed as Sleeping Beauty
and is accepted without release-year or original-title metadata. This exception
applies only to that filename, not other Sleeping Beauty films.

---

## Books, Disney Library and video library — version 0.9

Books & Stories and the Disney room have backgrounds without the three floor app
icons, larger covers and the same eight/twelve Jellyfin positions. Native app
controls are also hidden in those rooms. Selection starts on the books; Down from
the lower shelf selects Options, and Up returns to the books. Shelf edges still
switch rooms. Video rooms retain their apps.

The Kids room keeps its framed portrait and gains a video-library sign. The Disney
room was named Disneyteca at this point in development.

Backgrounds edited with the built-in imagegen tool:

- `app/src/main/assets/library-books.png`: eight larger books, no apps.
- `app/src/main/assets/library-disneyteca.png`: twelve larger books, no apps.
- `app/src/main/assets/library-kids-videoteca.png`: sign-text change only.

Each PNG had an accompanying `.provenance.json` recording its source and prompt.
Cover coordinates were aligned to the book fronts in each image.

---

## Disney Media — version 0.8

Books & Stories uses `library-kids-portrait.png`, the same framed-portrait background
as Kids. The fourth room, **Disney Media**, uses
`app/src/main/assets/library-disney.png`, edited with the built-in imagegen tool.
The full prompt was recorded in `app/src/main/assets/library-disney.provenance.json`.

Navigation cycles through **Books & Stories → Kids → Adults → Disney Media**.
Disney Media has two shelves of six positions and the same three apps as Kids.
Room switches preserve the row and enter at the opposite edge, including transitions
between four-column and six-column shelves.

The twelve films follow the reference photograph `PXL_disneylibrary.jpg`, left to right:

- Upper shelf: The Little Mermaid (1989), Sleeping Beauty (1959), Beauty and the Beast
  (1991), Brave (2012), The Princess and the Frog (2009), Mulan (1998).
- Lower shelf: Snow White and the Seven Dwarfs (1937), Cinderella (1950), Aladdin
  (1992), Moana (2016), Pocahontas (1995), Tangled (2010).

The launcher searches only the selected Kids Jellyfin libraries using paginated
movie requests. Matching accepts Portuguese, English and original titles and
requires the animated film's year to reject remakes and sequels. Artwork, progress
and links come from real Jellyfin items. Missing films or release-year metadata
leave a named unavailable position. OK opens refresh/library selection options.
At this stage, all twelve films still required device verification.

---

## Family portraits and Stremio — version 0.7

Kids displays the private source photograph `kids.jpg` in a wooden frame with a
light mat on the right wall, using `app/src/main/assets/library-kids-portrait.png`.
Books & Stories still uses the original Kids background at this stage. Personal
source photographs are excluded from the public repository.

Adults has **Jellyfin → SmartTube → Netflix → Stremio**. The private `couple.jpg`
photograph appears in a frame on the left wall. All four app bases sit within the
coffee-table edges. Stremio is visible only in Adults and resolves the installed
app (`com.stremio.one` or `com.stremio.two`). Kids and Books & Stories retain three
apps. Navigation crosses all four before switching rooms; entry from the right
selects Stremio. `app/src/main/assets/library-adults.png` was edited with imagegen,
with its prompt and coordinates in the accompanying provenance record.

---

## Books & Stories — version 0.6

Three rooms cycle left to right: **Books & Stories → Kids → Adults**. Edge
navigation preserves the row. Covers have no title strips or detail panel; only
real Jellyfin/Netflix progress is shown. The new room reuses the Kids background
and displays eight distinct random Jellyfin items from `Livros e historias`, with
accent normalization. Four books occupy each shelf. Selection is not restricted
to movies, series or started items. OK opens the item in Jellyfin. Re-entering the
room or refreshing in Options requests another random selection. The selected
room is saved.

---

## Adults and Kids libraries — version 0.5

The launcher uses two room backgrounds. Each view has four books per shelf: two
movies on the left and two series on the right. Ongoing items occupy the upper
shelf; recent additions, ordered by Jellyfin DateCreated, occupy the lower shelf.
The upper shelf combines a Jellyfin movie/series with a Netflix movie/series.
User-confirmed Netflix examples are Eat Pray Bark / Everwood in Adults and The
Loud House Movie / The Loud House in Kids. Covers, progress and links retain their
Watch Next data. Association is explicit because Android TV does not identify
Netflix profiles. Recent additions remain Jellyfin because the device exposes no
Netflix latest-additions channel. Ongoing episodes are grouped by series. Missing
positions remain explicitly unavailable without invented recommendations or progress.

On first use, `Movies (kids)` / `Shows (kids)` map to Kids and `Movies (18+)` /
`Shows (18+)` to Adults. Supported alternatives include `movies kids`, `tvshows kids`,
`movies +18` and `tvshows +18`. **Options → Libraries for this view** changes the
association. The existing Jellyfin session is preserved and the selected view is
saved. Rooms organise the launcher; external apps keep their own profiles. Netflix
opens its app, where the viewer selects the appropriate adult or kids profile.

- **Up/down:** move between apps, the lower shelf and upper shelf, preserving the column.
- **Left/right:** select books or apps; crossing an edge switches rooms and enters
  at the opposite edge of the same shelf.
- **OK on a book:** open its title in the corresponding app.
- **Down from apps, then OK:** open Options. **Back:** return to apps.

Build for the configured Home device with `bash build.sh -PhomeEnabled=true`.
Older notes follow. Historical files mentioned in those notes were removed during
the cleanup on 2026-10-20.

---

## Large-book library — version 0.4

The television has been removed completely. Its space is now a warm wooden bookcase,
with **eight large book positions**: four Netflix Continue watching titles on the
upper shelf and four unfinished Jellyfin films below. The covers are 244×264 / 244×255
scene units, approximately double the previous 110–133×139–150 cards in linear size.
Titles and progress are also larger. Unavailable positions are explicitly labelled
app shortcuts; no film titles or viewing progress are invented.

The selected book's full title, source, progress and opening action appear in a large
panel below the shelves. Circular app icons remain without visible labels. Use Up to
enter the books, Left/Right to select, OK to open, and Down/Back to return to apps.
Down then OK from the apps opens Options. Home remains Living Room on the configured box.

This supersedes the earlier television-preview layout documented below. The new
production plate is `app/src/main/assets/cozy-library.png`; its exact built-in imagegen
edit prompt is in `cozy-library.provenance.json`. These historical source images were removed during cleanup on 20/10/2026.

Build/update on this box: `bash build.sh -PhomeEnabled=true`, then `bash install.sh`.
The normal prototype variant is built with `bash build.sh` for stock devices.

---

## Living Room TV

A native Android TV launcher (`com.xsoulp.livingroom`) using **Cozy Smart TV Living Room Launcher.png** as its visual baseline. The warm room, dog, fireplace, shelves, and mountain/lake television are a baked background. Two real Netflix Continue watching titles occupy the upper bookcase shelf and three unfinished Jellyfin movies occupy the lower shelf; selecting a card displays its title, episode information, description when available, artwork, and progress **inside the room's television**.

Selecting Jellyfin shows up to two real latest library additions on that television. Netflix and SmartTube show the mountain/lake landscape when no verified latest-additions source is available. Their Continue watching history is never relabelled as newly added content. The three circular app icons launch their respective apps with OK. They use the installed apps' icons without visible names or action text; accessibility names remain available.

Version 0.3 uses a lightweight image-based 2.5D compositor with a perspective-mapped TV and subtle eased camera motion. The earlier low-poly room renderer has been replaced. The source PNG is untouched; the cleaned plate is `app/src/main/assets/cozy-room.png`. It was edited with the built-in imagegen tool to remove baked mock titles, logos, app cards, clock and weather. The exact prompt and asset provenance are in `app/src/main/assets/cozy-room.provenance.json`.

### Build the APK

Requirements: JDK 17 or newer (this workspace has Java 21), Android SDK Platform 36, and Build Tools 36.0.0. The Gradle wrapper downloads Gradle 9.6.0 with SHA-256 verification. Android Gradle Plugin is pinned to 9.4.0. First builds need internet to retrieve tools and dependencies; there are no third-party app dependencies.

```bash
cd family-shelf
bash build.sh
```

If the SDK components are missing, install them with Android Studio's SDK Manager or:

```bash
sdkmanager "platforms;android-36" "build-tools;36.0.0"
```

Set `ANDROID_HOME` if the SDK is outside `~/Android/Sdk`. `local.properties` is machine specific and ignored. The build runs Android Lint as well as compilation.

APK paths:

- `artifacts/living-room-tv-debug.apk` after `build.sh`.
- `app/build/outputs/apk/debug/app-debug.apk` after `./gradlew assembleDebug`.

This is a debug-signed sideloadable APK, not a production release. Pure Java and the native Android canvas require no ARM64 libraries, so the APK supports the box's 32-bit ARM userspace.

### Install and try

Enable network debugging and accept this computer's authorization prompt on the TV. The inspected box is at `YOUR_TV_IP:5555`:

```bash
bash install.sh YOUR_TV_IP:5555
## Or supply another address:
bash install.sh YOUR_TV_IP:5555
```

Alternatively, copy the APK to the TV and open it through a file manager with install permission. Find **Living Room** in the TV's Apps list. The installer opens it once for testing.

The normal build is an ordinary TV app. Installing it on a stock box does not change Home or disable system apps. This particular box was subsequently switched to the Home-enabled variant at the owner's explicit request; use `bash build.sh -PhomeEnabled=true` for updates while it is the default. The installer rejects a prototype-only update while Living Room is Home, to avoid removing the active Home entry point.

### Remote controls

| Control | Action |
| --- | --- |
| Left / right in the room | SmartTube → Jellyfin → Netflix → Options, wrapping at the ends |
| Up | Enter Continue watching |
| Left / right in the shelf | Previous / next poster, automatically changing five-card pages and updating the TV |
| Down in the shelf | Return to the app objects |
| Down in the room, then OK | Open Options without a Menu key |
| OK / Enter | Open selected app or shelf item |
| Long OK, Menu, or on-screen Options | Open options |
| Back | Return from shelf/Options to the room; stay in the room when it is Home |

The circular app targets show only installed app icons. A blue outline marks the selected app or shelf poster, matching the visual baseline. The TV shows the selected poster's details; the shelf and apps remain visible. Selection and room detail quality are saved. Returning from an app refreshes app availability, history, and latest Jellyfin additions.

Supported candidates include `org.smarttube.stable`, `org.smarttube.beta`, legacy SmartTube packages, `org.jellyfin.androidtv`, `dev.jdtech.jellyfin`, `org.jellyfin.mobile`, `com.netflix.ninja`, and `com.netflix.mediaclient`. TV launch intents are preferred; standard launch intents are the fallback. Missing, disabled, or inaccessible apps show a dialog with access to app settings. No store redirect or installation is forced.

### Jellyfin Continue watching

Open **Options → Connect / change Jellyfin account**. The server URL is editable and initially prefilled with the server detected from this box's existing Jellyfin configuration. Base paths such as `https://example.org/jellyfin` are supported.

The room needs its own user session even when the separate Jellyfin TV app is signed in. Choose **Get Quick Connect code**, then approve that code from a signed-in Jellyfin web client under **Profile → Quick Connect**. The room polls only while the connection screen is in front and stops after three minutes. If Quick Connect is disabled, an on-device username/password form is available. Passwords are never saved. The returned user token is encrypted with an Android Keystore AES-GCM key in private app preferences. Backup is disabled. Signing out revokes this room's session when the server is reachable; if offline, the UI reports that only local credentials were removed.

The shelf calls authenticated `GET /UserItems/Resume`, with a legacy `/Users/{userId}/Items/Resume` fallback on HTTP 404. The API bounds resume results to 12 records. The lower shelf requests only movies and shows the first three; episodes are excluded there. Completed, unstarted, invalid, and duplicate records are filtered. Server 100-nanosecond playback ticks become milliseconds for progress and time remaining. Episode cards retain series name and season/episode labels. Artwork uses the item's primary image or the series primary image for episodes, fetched with the token in an HTTP header, never an image URL.

There are no sample titles or synthetic progress values in the app. Empty libraries show an app shortcut; offline/authentication failures show a retry/reconnect explanation. Failed poster loads retain the actual item's title and progress with an explicit artwork placeholder.

**Opening an item:** the supported entry point in Jellyfin Android TV 0.19.10 is its exported StartupActivity with a canonical UUID `ItemId`. This opens item details. The intent uses `NEW_TASK` and preserves Jellyfin's existing task and credentials. Clearing that task caused intermittent navigation resets to Jellyfin Home on the inspected version. Choose **Resume** inside Jellyfin. The launcher does not claim an unsupported direct-autoplay intent, does not pass a token to Jellyfin, and does not stream media itself. The Jellyfin TV app must be signed in to the same server and account as the room. Other Jellyfin clients fall back to opening the app when the known TV entry point is unavailable.

### Netflix and SmartTube Watch Next

The room queries `content://android.media.tv/watch_next_program` under its own app identity, allowing only Netflix and known SmartTube package owners. Only visible Continue-type rows with a title and app intent are displayed; known completed positions are excluded. Image requests are bounded, and item intents are restricted to the owning app. It never writes Watch Next rows or copies invented history from another source.

**Options → Allow shared TV data** requests Android's ordinary `READ_TV_LISTINGS` permission. Whether that reveals other apps' Watch Next rows depends on the provider and firmware. AOSP normally restricts unprivileged access by row ownership; even granting the listings permission does not guarantee media history access. This app does not request or attempt to grant the privileged `ACCESS_ALL_EPG_DATA` permission, root the box, or scrape Netflix/SmartTube private storage.

When a provider is absent, denied, empty, or doesn't share these apps' data, the shelf explicitly shows **Open Netflix** / **Open SmartTube** app shortcuts without fake posters, titles, or progress. Zero accessible rows does not establish that those apps have no viewing history.

On this box, granting the normal TV listings permission exposed 27 rows. Three real Netflix Continue records passed the unfinished/browsable filters. No SmartTube Continue records were accessible, so SmartTube uses an app shortcut. These are observed device results, not a guarantee for other firmware or accounts.

### Television previews

The launcher's existing authenticated Jellyfin session calls `GET /Items/Latest?userId=…&limit=2&includeItemTypes=Movie,Episode,Series&groupItems=true`, with the legacy `/Users/{userId}/Items/Latest` route used only on HTTP 404. This is the server's latest-media ordering, including its per-user visibility/settings, separate from Continue watching. Up to two genuine records and posters are displayed. Empty, offline, unauthorized, or unsupported responses leave the mountain/lake landscape on the television.

Netflix and SmartTube do not currently expose a verified latest-additions feed to this launcher. Their real Watch Next Continue records remain on the shelf, while selecting either app shows the default landscape. No recommendations, unfinished history, sample media, clock, or weather are invented to fill the TV. The Netflix and SmartTube shortcuts continue to open their apps normally.

Shelf selection updates the television immediately from already-loaded metadata. Descriptions come from Jellyfin's `Overview` field with markup stripped; other sources show only metadata actually shared by their provider. Missing artwork retains the real title and progress. The TV panel is mapped to the photographed screen's perspective, so it sits inside the living room rather than covering the whole interface.

### Older-hardware budget

- One static 1672×941 background plate in RGB565, about 3 MB decoded; Low room detail decodes it at 836×471, about 0.75 MB. Text stays at native display resolution.
- Hardware-accelerated Android canvas, pre-rendered lighting and materials, perspective TV mapping, five shelf cards (two Netflix above, three Jellyfin films below), and native remote-accessible app and Options controls.
- No live room lighting, shadow maps, reflective passes, video backgrounds, imported 3D engine, or continuous redraw loop. Camera motion uses a short interruptible 280 ms animation on selection changes; the scene becomes idle afterwards.
- Poster images are bounded and sampled to small sizes, fetched on two worker threads. Up to 12 Jellyfin and 12 accessible Watch Next media records, plus two latest Jellyfin additions, are retained. Missing integrations use explicit app shortcuts.
- No background polling or wake lock. Work publishes only while the activity is current; camera animation stops when another app is in front.

These are design choices, not measured frame rate or thermal endurance claims.

### Inspected device

ADB reported Xiaomi `MiTV-AFKR0` / `jaws`, Android 11 (API 30), about 2 GB RAM, 1920×1080 output, and 32-bit ARM. The GL version property reports ES 3.2. SmartTube (`org.smarttube.stable`), Jellyfin (`org.jellyfin.androidtv`), and Netflix (`com.netflix.ninja`) all have resolvable TV entry points.

### Optional launcher replacement — after testing

The normal APK's HOME alias is disabled. To expose a Home candidate deliberately:

```bash
bash build.sh -PhomeEnabled=true
bash install.sh YOUR_TV_IP:5555
```

Then choose Living Room in Android's Default apps / Home settings, if the firmware exposes that setting. Installing the APK does not choose it for you. Some Xiaomi firmware restricts Home selection; this prototype does not bypass that restriction.

To restore the default prototype build, first select Google TV as Home in Android settings, then:

```bash
bash build.sh
bash install.sh YOUR_TV_IP:5555
```

On the inspected Xiaomi firmware, choosing the Home role alone did not override the stock launcher's higher-priority Home filters. After the owner explicitly requested replacement, `com.google.android.apps.tv.launcherx` and `com.google.android.tungsten.setupwraith` were disabled **for user 0**, with neither package uninstalled. Living Room's `.Home` alias is now the resolved and observed foreground Home activity. Do not make these firmware-specific changes merely to test the normal prototype.

To restore the stock launcher on this box:

```bash
bash restore-google-tv.sh YOUR_TV_IP:5555
```

The script re-enables both stock packages, restores the Google TV Home role/preference, and presses Home. Restore Google TV before installing a build with its Home alias disabled. While Living Room is Home, the Back key returns from the shelf or Options to the room, and stays in the room when already at Home. Home also returns to the room's app navigation.

### Acceptance checklist

1. On a stock device, Home still opens Google TV after installing the normal build.
2. Left/right selects app objects; Up enters the shelf; Down returns. Camera motion settles smoothly, including rapid changes, and the TV follows shelf selection.
3. OK opens each corresponding app. Returning restores the room and selection.
4. Down then OK opens Options without a Menu key. Menu and long OK also open Options. The Low room detail setting survives app restart.
5. Sleep/wake and leaving/reopening the app do not disrupt the room compositor.
6. On an emulator without the media apps, OK shows the unavailable-app dialog. Use an emulator for this case rather than removing existing TV apps.

Build validation and actual device checks are recorded in `VALIDATION.md` when available.

Device smoke tests use a small custom instrumentation runner without test framework dependencies:

```bash
./gradlew assembleDebug assembleDebugAndroidTest lintDebug -PhomeEnabled=true
~/Android/Sdk/platform-tools/adb -s YOUR_TV_IP:5555 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
~/Android/Sdk/platform-tools/adb -s YOUR_TV_IP:5555 shell am instrument -w -e mode live com.xsoulp.livingroom.test/com.xsoulp.livingroom.RoomSmokeInstrumentation
```

The command above builds the Home variant for this box. Omit `-PhomeEnabled=true` when testing as an ordinary app on a stock device. Live mode requires an already-paired Jellyfin account. It validates real unfinished records and poster decoding, then checks television preview modes, D-pad shelf navigation, Options through Down/OK, Menu, and missing-app handling without disabling existing apps. Synthetic model fixtures exist only in the test APK and never populate the production shelf. Playback checks and their limits are recorded separately in `VALIDATION.md`. To deliberately open and briefly resume each real unfinished item, run:

```bash
~/Android/Sdk/platform-tools/adb -s YOUR_TV_IP:5555 shell am instrument -w -e mode playback com.xsoulp.livingroom.test/com.xsoulp.livingroom.RoomSmokeInstrumentation
```

This controls the TV during the test and advances real playback positions slightly. Each item starts from Home, uses Jellyfin's native Resume button, and is paused after server-side position checks.

### Integration references

- [Jellyfin SDK authentication](https://github.com/jellyfin/jellyfin-sdk-kotlin/blob/master/docs/guide/authentication.md)
- [Jellyfin latest-media endpoint](https://github.com/jellyfin/jellyfin/blob/master/Jellyfin.Api/Controllers/UserLibraryController.cs)
- [Jellyfin resume endpoint implementation](https://github.com/jellyfin/jellyfin/blob/master/Jellyfin.Api/Controllers/ItemsController.cs)
- [Installed Jellyfin TV 0.19.10 item entry point](https://github.com/jellyfin/jellyfin-androidtv/blob/v0.19.10/app/src/main/java/org/jellyfin/androidtv/ui/startup/StartupActivity.kt)
- [Android Watch Next](https://developer.android.com/codelabs/tv-watch-next)
- [AOSP TV provider access filtering](https://android.googlesource.com/platform/packages/providers/TvProvider/+/master/src/com/android/providers/tv/TvProvider.java)

### Diagnostics

```bash
~/Android/Sdk/platform-tools/adb -s YOUR_TV_IP:5555 logcat -d -s LivingRoom AndroidRuntime
```

Lint report: `app/build/reports/lint-results-debug.html`.
