# Family Shelf — validation records

Build and device results for Family Shelf, currently version 0.15.2.
Use the [README](README.md) for current setup and [project history](HISTORY.md)
for earlier design notes. Historical entries retain technical identifiers and
observed media names from their original checks. Older device results do not
establish that a later version was installed or tested on TV.

## Creator credit and public APK onboarding — version 0.15.2

- Added © xsoulp 2026 through a non-interactive window overlay on both Activities
  and all app-created dialogs, plus the aircraft view and browser preview. The
  credit stays outside the scrolling/camera transform. ViewGroupOverlay avoids
  changing dialog measurement or handling user input.
- Both public APK variants compile and pass lint (zero errors, 26 warnings; the
  added view-constructor warning concerns the programmatically created credit).
  ZIP inspection excludes private
  photo, outdoor music and selected-location inputs. Signature verification confirms
  both APKs use the same dedicated public development certificate; manifest checks
  confirm Home is enabled only in the Home artifact. Checksums are generated.
- All 99 JVM regression tests passed during this update. The browser preview's
  existing self-test passed 69 checks with the credit present. README/reference
  relative links, workflow YAML and every workflow shell step were checked locally.
- Public APK workflow builds from an empty private-asset directory, checks outputs
  and uploads both variants plus SHA-256 checksums. Its development key is explicitly
  public and separate from the owner's signing identity. The README explains artifact
  sign-in, downloading, installation, compilation and private customization.
- The TV remains unreachable, so no native credit screenshots or TV deployment are
  claimed. GitHub-hosted execution is separate from the local checks; artifact
  availability requires a successful workflow run after the workflow is pushed.

## Hourly church bell and realistic aircraft — 07/11/2026, versions 0.15.0–0.15.1

- Downloaded Bell 1 O'clock by Joseph SARDIN & Axeline T. (BigSoundBank, CC0).
  The complete recording was converted to mono 22,050 Hz PCM16: 7.44 seconds and
  328,104 decoded bytes. The recorded strike repeats at 17% gain every 2.4 seconds,
  allowing its original decay to overlap. Source/download hashes are saved.
- Hour-boundary logic counts 1–12, including noon/midnight, in the configured
  scene zone. Opening/waking or changing time never replays missed hours. Shared
  saved markers suppress duplicate Activity strikes. Mute/focus/screen/lifecycle
  checks cancel pending audio. The final requested scope is screensaver only;
  the one-bell preview opens that scene, and normal room/video playback is gated out.
- All 99 JVM tests passed, including every hour, repeat suppression, muted and
  late boundaries, recreation, clock jumps and both Lisbon daylight-saving changes.
  Private main/test APKs built and lint passed (zero errors, 25 warnings).
- Version 0.15.0 was installed. Native diagnostics confirmed successful SoundPool
  loading; a preliminary one-strike preview incremented the strike counter once.
  Final screensaver-only device verification was not completed. The test harness
  initially inspected a background MainActivity instead of the active Home alias;
  it was updated to inspect the active instance. A shared audio-release settling
  timestamp also avoids treating a sibling Activity's old music as another app.
- The lower aircraft caption is removed; only callsign and airline remain above.
  Data-provider/license credits are available in the video-aircraft About dialog.
- Version 0.15.1 replaces vector planes with an original generated RGBA manga
  airliner. Tail/stripe colour filters are clipped to registered polygons; both
  scenes use the same renderer. Prompt and provenance are saved with the asset.
- The final 0.15.1 private APK preserves the original photo, song and weather
  configuration byte-for-byte. A separate public build excludes those inputs and
  includes the bell and aircraft sprite. Device installation and the updated native
  aircraft render check could not run because the box became unreachable on the
  local network. The prepared private APK remains in ignored artifacts; it is not
  represented as installed, and no live-flight screenshot was fabricated.

## Manga room with painted app balls — 07/11/2026, version 0.14.0

- Built-in image generation repainted the continuous 1024 x 1536 Kids room with
  manga linework and shading. The six empty shelf compartments retain their live
  cover registration. Three unbranded balls, contact shadows, blocks and a train
  are integrated into the image; prompts and source/scene coordinates are saved.
- Native ball bitmap/shadow rendering was removed. Transparent 124-unit app views
  align with painted centers at scene (645,2364), (838,2364), (1029,2364).
  They draw only a 52-unit selection outline; left-to-right apps remain Jellyfin,
  Red Descent, Netflix. App names persist in accessibility descriptions.
- All 92 JVM tests passed. Private build and lint passed (zero errors, 25 warnings).
  Installed version 0.14.0 on the TV, preserving private photo, song and location
  byte-for-byte. Separate public build passed and excludes those private inputs.
- Device assertions checked all three floor selection positions and returning to
  shelves. Native captures were visually reviewed for circle/ball alignment,
  complete toy silhouettes, shelf/covers alignment and compact foreground covers.
  A tap on the middle painted ball opened Red Descent; a tap on the left opened
  Jellyfin, confirmed from the resumed Activity. Returned to the launcher afterward.
- Published captures contain only Kids artwork and media covers, with no account
  information or personal room photos. Older captures remain historical examples.

## Room depth, unlabelled Kids and textured sprites — 07/11/2026, versions 0.12.4–0.13.1

- Kids HUD, section badges, counter, navigation hints, room/options text buttons
  and printed ball names were removed. Covers, the decorative sign, progress and
  focus outlines remain; Menu/long OK still opens settings. A native capture was
  reviewed to confirm the clean room. Removed HUD touch targets no longer intercept
  taps on covers underneath their former positions.
- Both rooms render shelf books with a projective pose. Selection interpolates
  to a straight centred foreground cover over 420 ms; previous covers return.
  After device review, foreground height was reduced from 620 to 420 scene units
  and background dimming halved, keeping neighbouring books visible. Covers are
  requested up to 480 pixels wide with bounded decoding for foreground detail.
- All 92 JVM tests passed for the final compact geometry, including convex poses
  across every Kids shelf and intermediate progress, straight centring and aspect
  preservation. Private build and lint passed with zero errors and 25 warnings.
- Native captures confirm the smaller selected cover, changing selection and
  returning to floor apps. Day/night screensaver captures show the generated
  transparent manga cloud sprite with matching lighting. Captures two seconds
  apart show changed cloud/clock pixels. Original alpha is retained; generation
  prompts and provenance are saved. No new landscape plate was generated.
- Version 0.13.1 replaces the procedural ball surface with a shared generated felt
  texture and native soft contact shadows. Installed on the TV and visually reviewed
  in a native Kids-floor screenshot. Build/lint passed; the earlier 92-test
  geometry suite was not rerun solely for the ball texture change.
- Private updates preserve original photo, music and location byte-for-byte. The
  separate public APK excludes these private inputs. Published examples include
  only Kids and landscape views; personal Adults captures stay in ignored artifacts.

## Aircraft sound replacement — 07/11/2026, version 0.12.3

- Downloaded Airplane, pass #4 by Dj Lio from BigSoundBank; the source explicitly
  permits app redistribution and editing under CC0. No synthesized audio was used.
  Download and packaged hashes are recorded in docs/whoosh-source.json.
- Full sound decodes successfully: 10.848 seconds, mono 22,050 Hz PCM16, 478,398
  decoded bytes. Conversion keeps the sample below SoundPool's per-sound memory
  limit. Peak is -7.7 dBFS; no clipping was found. Playback gains remain unchanged.
- Private Home-enabled APK built and installed as 0.12.3. APK inspection confirmed
  exact packaged effect bytes and preserved private photo, MP3 and weather config.
- The rebound native service reported whooshReady=true and active Netflix playback
  after installation. No playback controls were sent and the launcher was not
  opened over the video. Actual subjective listening remains a user check.
- Flight logic and cue timing are unchanged; the existing 89-test result belongs
  to version 0.12.2 and was not rerun for this audio asset replacement.

## Jellyfin STATE_NONE playback compatibility — 07/11/2026, version 0.12.2

- Started a requested cartoon episode in Jellyfin Android TV. Client logs confirmed
  advancing direct-play progress while Android MediaSession remained STATE_NONE;
  the version 0.12.1 detector therefore correctly stayed inactive under its native
  state policy, exposing a client compatibility gap.
- Added a read-only server fallback only for a supported Jellyfin media session
  with absent/NONE Android state. It matches the signed-in account, a local box
  address, client type, a video item, explicit unpaused state, positive position
  and a playback check-in within 20 seconds. Checks run about every three seconds;
  failure or ambiguity cannot authorize an aircraft window.
- All 89 JVM tests passed, including local/account/client matching, stale/future
  reports, paused/stopped/audio-only records, missing pause state and mapped IPv4.
  Private build and lint passed (zero errors, 25 warnings). Original private assets
  were preserved byte-for-byte; a separate public APK excludes them.
- Installed the private update without replacing the Jellyfin player. Native
  diagnostics confirmed jellyfinPlaying=true, jellyfinServerFallback=true and
  active=true with no playback-check error. The flight feed polled successfully
  and the whoosh loaded. No extra Android permission was required.
- A later pause/resume check was interrupted by loss of the box connection and is
  not recorded as passed. A real aircraft capture over Jellyfin has not yet been
  obtained. Initial screenshots during playback were black; no substitute frame
  or simulated flight was presented as a real capture.

## Jellyfin aircraft overlay — 07/11/2026, version 0.12.1

- Private Home-enabled APK, all 82 JVM tests and Android lint passed (zero errors,
  25 warnings). Policy checks cover Netflix and all three Jellyfin clients used by
  launcher shortcuts, paused/unsupported playback, unknown foreground, mismatched
  background sessions, permission gating and sleeping screens.
- Installed the private update with the original photo, outdoor MP3 and weather
  configuration preserved byte-for-byte. Existing listener/overlay access survived.
  The rebound service reported active Netflix playback, successful polling and
  whoosh loading without interrupting the video. No new permissions were needed.
- Before this update, production Netflix diagnostics recorded real aircraft
  windows and whooshes, including TAP56DP. The user confirmed seeing an aircraft.
  Both direct and on-device screenshot attempts returned empty images during
  Netflix playback; no Netflix-video screenshot was obtained or published.
- A separate public build passed, excluding the private photo, supplied song and
  household location from the APK.
- Jellyfin playback and a real aircraft capture over Jellyfin still require a
  device check with a Jellyfin video playing; automated policy tests alone do not
  establish that a client publishes its playback session or permits screenshots.

## Netflix aircraft and manga styling — 06/11/2026, version 0.12.0

- Home-enabled private main/test APKs built; all 79 JVM tests passed. Android lint
  completed with zero errors and 25 warnings. New tests cover Netflix package and
  playback/permission/foreground gating, operator families and unknown callsigns.
- Five native checks passed for off-screen rendering of the four operator styles.
  The saved example sheet was visually reviewed for ink outlines, shading and
  orange, navy/yellow, grey/yellow and green/red tails. It contains illustrative
  callsigns and is explicitly labelled as design examples, not live detections.
- Installed version 0.12.0 while preserving settings, the original private photo,
  supplied music and location. The installed APK assets match the private inputs.
- Android bound the playback observer, granted overlay access and loaded the
  whoosh successfully. With no active Netflix playback, diagnostics confirmed no
  polling, aircraft window or effects. A real aircraft over active Netflix video
  has not yet been verified; no simulated flight was injected into production.
- A separate default public build passed; ZIP inspection confirmed the private
  photo, supplied music and household weather configuration are excluded.
- Only notification-listener playback state and optional accessibility foreground
  package names are used. The overlay requests neither focus nor input, and the
  service has no Netflix transport controls or outdoor music player.

## Quiet outdoor music and aircraft whoosh — 06/11/2026, version 0.11.1

The private TV build plays the supplied Bonito, Bonito MP3 in a loop during the
screensaver, at 7% playback gain with a short fade-in. Room/outdoor music shares
one audio-focus lease, avoiding competing players. The original file is unchanged
and stored only in ignored private assets; the private photo/location remain there.

A per-plane cue triggers one original downloaded Air whoosh WAV by PyranoStudios
(OpenGameArt, CC0) as the aircraft approaches the centre of the sky. It plays once
at 16% gain. Poll updates and repeated draw frames cannot retrigger the same cue;
hiding the scene cancels pending cues. Mute, screen-off and focus loss silence both
music and effects. Playback resources are released when the launcher closes.

Validation:

- Home-enabled app/test APK builds, all 72 JVM tests and Android lint passed
  (zero errors, 24 warnings).
- Both the original supplied MP3 and downloaded WAV decode fully. The private APK
  MP3 bytes match the supplied file, and the original Adults-room photo is retained.
- JVM tests cover shared focus across three music modes, no restarts within one
  scene, outdoor mute/focus/foreground behaviour and once-per-aircraft cue timing.
- The first native test confirmed actual music playback, looping/full-track decode
  and successful SoundPool loading. An immediate off/on toggle exposed a race with
  AudioFlinger's report of the just-released player. Unmute now waits briefly for
  that report to settle, preserving the guard against interrupting another app.
- After that fix, native assertions passed for selecting the private track,
  MediaPlayer playback/looping/full-track decoding, successful SoundPool loading,
  immediate mute and restored playback after unmute. The longer combined preview
  run was interrupted by a foreground-app transition and is not recorded as a
  fully passed UI suite. QA now tracks the resumed launcher Activity and reports
  assertion failures on the instrumentation thread instead of crashing its main
  thread. Further intrusive checks stopped when Netflix was in the foreground.
- Version 0.11.1 was installed with settings and the private photo preserved. The
  default public APK was built separately and confirmed to exclude the supplied
  song, personal photo and selected weather configuration. Only the CC0 effect
  is redistributed; no generated audio was used.

## Outdoor screensaver, weather and bridge aircraft — 06/11/2026, version 0.11.0

Implemented the requested two-minute idle screen using paired generated Lisbon
manga landscapes. The source images contain a blank bell-tower dial; native Canvas
draws the real Europe/Lisbon clock, including seconds. Cloud cover/wind drive moving
clouds, with rain/snow/fog overlays from current Open-Meteo model conditions. Daily
sunrise/sunset selects the matching day/night plate. Weather values retain source
timestamps and have explicit saved/unavailable states. Artwork prompts and dial
coordinates are in [screensaver-image-prompts.json](docs/screensaver-image-prompts.json).

The first wake gesture is consumed, Home returns to the room, dialogs postpone
entry and screen-off/pause stops drawing and polling. Existing room music pauses
while the outdoor scene is visible. Location configuration stays in local ignored
assets and the private build preserves the original Adults-room photograph.

ADSB.lol's free public nearby-aircraft endpoint responded successfully without a
key. Only recent airborne positions can trigger a plane animation when entering
or crossing an approximate 2.8 km by 800 m bridge corridor. Short-gap segment checks
catch crossings between polls; stale/ground/invalid data, implausible jumps and
repeat reports are rejected. Distinct later passes can trigger again. Polling runs
only while visible, with backoff and numeric Retry-After handling. Coverage and
sampling can miss a physical overflight; no random aircraft substitute for data.

Validation:

- Home-enabled main/test APK builds, all 66 JVM tests and Android lint passed
  (zero errors, 24 warnings).
- New JVM checks cover the exact 120-second boundary, reset/wake semantics,
  foreground/screen/dialog gating, source timestamps and stale weather, solar
  day/night transitions, invalid data/coordinates and flight corridor detection,
  interpolation, duplicate suppression and distinct return passes.
- All 14 native outdoor checks passed for day/night previews, first OK down/up being
  consumed, unchanged shelf selection, Home return, foreground/pause and screen-off/on behaviour.
- A real unattended idle check kept the room visible before the timeout and showed
  the screensaver after two minutes. Native captures were visually reviewed.
  Two captures three seconds apart have different cloud-band and clock-dial pixels,
  confirming live motion. A current-weather preview opened through the actual
  Options menu remained visible after 19 seconds.
- The TV's weather and flight requests succeeded; on-screen weather had a source
  update time and the flight service reported a fresh feed. A follow-up passive
  capture caught a production animation labelled TAP473H, triggered by live API
  data, after approximately 42 seconds of watching the TV. The unmodified native
  screenshot was visually verified and saved as a public landscape-only example.
  Event filtering and crossing geometry are also covered by controlled fixtures.
- Version 0.11.0 was installed with existing app data retained. The private APK
  contains the original photo byte-for-byte and the selected weather location.
  Screenshots, reports and private APKs remain in ignored `artifacts/`.
  Published day/night examples contain only the landscape and screensaver UI.
- A separate default public APK build passed. ZIP inspection confirmed it contains
  the scenic art while excluding both the personal photo and selected-location
  configuration. Private asset inputs were not committed.

## Optional private Adults-room photo — 06/11/2026, version 0.10.4

The renderer can load an optional `adult-room-photo.jpg` from local private assets
and display it within the existing Adults-room frame. It uses the original photo
with a centered crop; the room background, English sign and shelf geometry remain
unchanged. The source JPEG is never sent through image generation or committed.
Builds without the optional local asset retain the fictional public example.

Validation:

- Home-enabled private APK build, all 46 JVM tests and Android lint passed
  (zero errors, 22 warnings).
- The JPEG embedded in the private APK was verified byte-for-byte against the
  original local photo. The source is held in the ignored `.private/room-assets/`
  directory, and private APKs/captures are stored under ignored `artifacts/`.
- A separate default public build succeeded and ZIP inspection confirmed that it
  excludes the personal photo asset.
- The private version 0.10.4 APK was installed on the TV box without clearing app
  data. The Adults-room screenshot was visually reviewed and shows the original
  photo in the existing frame. Version code 24 was confirmed and no launcher
  AndroidRuntime crash was found. Device captures remain private under
  `artifacts/screenshots-0.10.4-private/`.

## Name-branded tennis ball shortcuts — 05/11/2026, version 0.10.3

Kids floor shortcuts are opaque felt-coloured tennis balls with cream curved seams.
Jellyfin, Red Descent and Netflix are printed directly on the balls as brand names;
app artwork and separate captions are removed. Focus rings, app targets and
accessibility labels remain in place. The public design preview uses the same
name-only treatment and its example screenshot has been refreshed.

Validation:

- Home-enabled main/test APK builds, all 46 JVM tests and Android lint passed
  (zero errors, 20 warnings).
- All 78 browser checks passed, including six checks for app names printed inside
  the balls and the absence of icon SVGs/images in each app ball.
- Installed version 0.10.3 with existing settings preserved and rebooted again.
  The launcher returned as Home automatically. Captures at 20 and 45 seconds
  after Android boot completion show the name-only balls and all twelve Disney
  covers without opening Jellyfin. Home resolution and focused window were
  confirmed; the final AndroidRuntime check found no launcher crash.
- Native D-pad checks crossed the left shelf boundary into Adults and returned
  from its right boundary, then repeated the round trip using the floor app row.
  Both checks passed. Saved captures were visually reviewed for Books & Stories,
  Kids media, Disney, floor balls and Adults; all visible cover slots had artwork.
- Actual TV captures and the reboot report remain private under
  `artifacts/screenshots-0.10.3/`. The published screenshot is a design example.
  This was a normal reboot with the network available; delayed-network boot
  recovery was tested with the local failure/retry fixtures, not induced on TV.

## TV installation and first reboot check — 05/11/2026, version 0.10.2

Installed with existing app data preserved, then rebooted the Xiaomi TV box.
Family Shelf returned as Home automatically, confirmed by the focused window and
resumed activity. Captures taken 20 and 45 seconds after Android reported boot
completion show all twelve Disney covers without opening Jellyfin. Home resolution
still points to Family Shelf and the final AndroidRuntime check found no launcher
crash. Evidence is saved privately in `artifacts/screenshots-0.10.2/`.

This verifies a normal reboot with the network available. It does not reproduce
an intentionally delayed network connection or server outage at boot.

## Startup shelf and poster recovery — 04/11/2026, version 0.10.2

Code inspection found that shelf metadata was fetched on resume and failed poster
downloads were silently discarded. There was no scheduled recovery while the
launcher remained on screen. Opening Jellyfin and returning caused a new resume
refresh, consistent with the reported boot symptom; a failing boot was not captured.

Temporary network and server errors now retry automatically after 2, 4, 8, 16,
32 and at most 60 seconds. Poster failures retry individually on a separate worker
pool. Pausing, changing requests or switching the screen off cancels scheduled
callbacks; screen-on refreshes the room. Jellyfin HTTP 401/403 and missing images
(404) are not retried as temporary network faults.

The bounded app-private startup cache stores actual shelves, saved progress and
Jellyfin covers. It restores them before live metadata loading, retains saved
shelves during transient failures, separates account/room/library/Netflix-selection
scopes and clears on sign-out. Credentials are not serialized into cache records.
Initial saved-cover reads use disk only; they do not queue duplicate network work.
No saved progress is treated as evidence of current playback until refreshed.

Validation:

- 46 JVM tests passed, including a loopback server that returns 503 then recovers
  using the launcher's existing authentication, cache reads through a new cache
  instance, scope isolation, corrupt/oversized data, file limits, sign-out cleanup,
  transient/permanent failure classification and bounded retry delays.
- Home-enabled main and instrumentation APKs built successfully; Android lint
  completed with zero errors and 20 warnings.
- Read-only ADB inspection confirmed that the box still has version 0.10.0.
  No installation, app launch, playback, remote input or reboot was performed.
  Native boot/network recovery remains pending device verification.

## Room edges and floor app toys — 04/11/2026, version 0.10.1

Both left and right boundaries of every Kids shelf now switch to Adults. Adults
boundaries return to Kids. Shelf entry uses the opposite horizontal end, retains
upper/lower row parity and restores the last Kids section. The same edge behaviour
works on both app rows, retaining app focus and handling three Kids/four Adults apps.

Kids Jellyfin, Netflix and the existing game shortcut are rendered as selectable
toy balls on the rug using native Canvas gradients, curved panel lines, shadows
and the installed app artwork. Their positions track the room camera. Back, or
Down after the last shelf, moves to the floor; Up restores the selected shelf.
HUD controls stay fixed. The browser design preview is updated with matching
room transitions and floor toys; the new floor screenshot is labelled as a preview.

Validation:

- 35 JVM tests passed, including six new room-edge tests across all six Kids
  rows, opposite-edge entry, four/six-column returns and floor-camera geometry.
- Both APKs and updated device instrumentation compiled successfully. Android
  lint completed with zero errors and 20 warnings.
- All 72 browser preview checks passed, covering shelf boundaries in both
  directions, reverse crossings, floor-app reachability, visible toy positions
  and music selection.
- Native instrumentation now checks shelf and app edges in both rooms, but was
  compiled only. No TV installation or remote input was performed for 0.10.1.
- Source-file privacy checks and local documentation links were reviewed before
  publication. Toy controls are code-drawn UI; no new raster asset generation or
  music download was needed for this update.

The previous 0.10.0 update was installed after its preview approval, and four
actual TV captures were saved locally under `artifacts/screenshots-0.10.0/`.
The new floor/edge behaviour remains pending device verification.

## Continuous Kids bookcase and downloaded music — 04/11/2026, version 0.10.0

Implemented the approved local preview as a single 28-cover, six-row Kids scene:
eight story books, eight Kids media items and twelve Disney films. Four/six-column
transitions keep the nearest horizontal position. Up/Down pans within the same
2508-unit-tall bookcase; horizontal Kids shelf edges remain in that room. Adults
keeps its existing shelves. Room controls, app shortcuts and persisted legacy-room
migration are updated. Netflix artwork lookup accounts for the combined row offsets.

Background music uses the original MP3 downloads of Music Box Theme and Darkest
Child by Kevin MacLeod/Incompetech under CC BY 4.0. The generated trial audio was
removed at the user's request. Sources/checksums and author/license credits are in
[music-sources.json](docs/music-sources.json) and
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md), with in-app Help credits.

Playback is quiet, starts with a fade, responds to foreground/screen state and
Android audio focus, and has a saved on/off setting. Navigation within Kids does
not restart its music. Opening another activity releases playback. Existing music
from another app is left alone.

Local validation:

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
  -PhomeEnabled=true` passed with the final downloaded MP3 resources.
- 29 JVM tests passed: 14 existing HTTP/film-selection checks, six bookcase
  navigation/geometry checks and nine audio lifecycle/focus policy checks.
- The browser preview passed 36 checks covering navigation, rendered camera
  position, explicit music enable/mute and switching room/track selection.
- Both original MP3 downloads decode fully and match their recorded SHA-256 hashes.
- Updated native device instrumentation compiles for the two-room layout, but has
  not been run on a TV. Actual native rendering, listening volume and platform
  audio-focus callbacks remain device-verification work.

No TV connection, installation, input events or playback were performed for this
update. Version 0.9.10 remains the last version installed in this session.

## Jellyfin compatibility and recent-movie exclusion — 01/11/2026, version 0.9.10

Read-only inspection of the configured server's public info identified Jellyfin
12.4.0. Its published OpenAPI includes `/UserViews` and `/UserItems/Resume` but
omits `/Users/{userId}/Views`, which the launcher previously called. Existing
launcher preferences still contain the configured account and view selections;
private values and the server schema export are excluded from the repository.

Requests now include the session token in `Authorization: MediaBrowser …,
Token="…"`, rather than relying solely on the legacy `X-Emby-Token` header. The
server's authentication implementation gates legacy token headers behind
`EnableLegacyAuthorization`. Library discovery uses the current route with a
404-only legacy fallback. Ongoing movie/episode requests use the resume route
with the same user and selected parent-library scope. HTTP 401/403 are not retried
as legacy requests and produce session/permission guidance.

The recent-movie selector excludes every Jellyfin ongoing movie ID returned for
the current view, including compact/hyphenated UUID equivalents, and the confirmed
Netflix movie shown in its ongoing row. It walks the remaining date-ordered
candidates to fill both recent movie positions. No alternatives leaves explicit
empty positions. Different Jellyfin films with the same name retain their distinct
IDs; cross-provider Netflix matching uses normalized titles because IDs differ.

Validation:

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
  -PhomeEnabled=true` passed.
- 14 local JVM tests passed: eight HTTP-client tests exercise standard token auth,
  server base paths, modern/legacy routes, user/library/type filters, and distinct
  HTTP 401/403 behaviour; six selection tests cover exclusions, UUID formatting,
  backfilling, provider matching, duplicate candidates and empty alternatives.
- The first test compile used a JDK-specific HTTP server class unavailable on the
  Android test compile classpath. The test fixture now uses a portable loopback
  `ServerSocket`; the final Gradle run passed all tests and lint.
- The main and test APKs were built locally. No app installation, device
  instrumentation, UI interaction or playback was performed. Authenticated live
  shelf loading with the TV's encrypted session remains unverified until deployment.

API references: [Jellyfin authorization implementation](https://github.com/jellyfin/jellyfin/blob/master/Jellyfin.Server.Implementations/Security/AuthorizationContext.cs),
[user views controller](https://github.com/jellyfin/jellyfin/blob/master/Jellyfin.Api/Controllers/UserViewsController.cs),
[resume/items controller](https://github.com/jellyfin/jellyfin/blob/master/Jellyfin.Api/Controllers/ItemsController.cs).

## English interface and documentation — 28/10/2026, version 0.9.9

Translated all built-in UI labels, dialogs, navigation hints, accessibility
strings, room signs, example interface captions and historical documentation into
English. The app and remote-button service now display the Family Shelf name.
English Disney catalogue labels keep Portuguese/original-title matching aliases.
Books & Stories also accepts English library names alongside the legacy library key.
External Jellyfin/Netflix titles and poster artwork retain their source language.

Validation completed locally:

- Main and instrumentation Java compilation and Android Lint passed.
- Home-enabled main and test APK builds passed with the final room assets.
- 46 offline catalogue checks passed, including bilingual matching, the existing
  filename exception, release-year handling, remake and sequel rejection.
- English UI string audit, documentation links, shell syntax, XML and JSON checks
  passed. Room signs and both edited example screenshots were visually reviewed.
- Published PNGs omit source metadata and embedded thumbnails; fictional portraits
  remain in place. Prompts are in [english-image-prompts.json](docs/english-image-prompts.json).

The Android signing step needed access to the local Android debug-key directory;
it succeeded after the environment permission request. No ADB connection, TV
installation, remote input, playback or device instrumentation was performed.
The TV remains on its previously installed version, as requested.

## Initial public repository — 28/10/2026

Prepared Family Shelf for publication with generic profile labels, no prefilled
private Jellyfin address, and an explicit TV target in installation scripts.
Personal room photographs were replaced with AI-generated fictional portraits;
private originals and device exports are excluded. Two reviewed example images
are included in the README. See [privacy review](docs/PRIVACY.md).

Main and instrumentation APK builds and lint passed after the code cleanup.
The separate public checkout also passed assembleDebug with its final room assets;
the packaged assets match the reviewed files byte for byte. Shell syntax, XML,
JSON, documentation links and the staged privacy scan passed. No TV installation
or new device instrumentation run was performed for this publication.


## Red Descent shortcut — 25/10/2026, version 0.9.8

The Kids central shortcut now resolves `pt.reddescent.game` and displays its
installed icon and Red Descent accessibility/fallback name. Replaces Lado a Lado
at the former SmartTube position. Adults and global remote mappings are unchanged.
Updated existing instrumentation expectations for room switches.

Verified the game package and exported Leanback launcher alias against
a local `RedDescent-debug.apk` using aapt. Main and
instrumentation Java sources compile against Android SDK 36; manifest XML parses.
Follow-up with restored access: Gradle assembleDebug, assembleDebugAndroidTest
and lintDebug all passed. Installed version 0.9.8 with Home enabled on
YOUR_TV_IP:5555, preserving app data. The TV resolves the installed game to
`pt.reddescent.game/com.godot.game.GodotAppLauncher`. UI dump confirms the Kids
central target is `Red Descent, available`, alongside Jellyfin and Netflix.
The full device instrumentation run failed while waiting for window input focus;
complete navigation and actual shortcut launch were not confirmed in that run.
APK: `artifacts/living-room-tv-0.9.8-home-debug.apk`.

---

## Kids game shortcut — 24/10/2026, version 0.9.7

Replaced the Kids room's SmartTube shortcut with the real installed Lado a Lado
icon and launch intent (`com.xsoulp.sidebyside`). A native opaque circular overlay
covers the baked SmartTube emblem and label; the supplied room background remains
unchanged. The adults room retains SmartTube. Room switches refresh both launch
intents and artwork so neither room keeps the other room's shortcut.

Gradle APK/test builds and lint passed (0 errors, 19 warnings). Installed with
preserved data. Device `mode kids-game`: **OK (219 checks)**, including game vs
SmartTube resolution, room-specific icon/name behaviour and existing navigation.
The initial run hung inside Android's startActivitySync while reusing the Home
alias; inspected the debug thread stack, stopped that run and fixed the test to
create a fresh activity task. The rerun passed and saved an app-rendered preview:
`artifacts/kids-lado-a-lado-0.9.7.png`.

After restarting Home, tapping the actual central Kids icon opened
`com.xsoulp.sidebyside/.MainActivity`, confirmed by the focused window. Returned
Home and reconnected the global remote-button service after instrumentation.
APK: `artifacts/living-room-tv-0.9.7-home-debug.apk`.

---

## YouTube remote key — 24/10/2026, version 0.9.6

Added YouTube (BUTTON_3, scan code 425 in the device's Xiaomi layout) -> SmartTube,
using the same global key-filter service as the confirmed Netflix and Amazon
buttons. Installed both APKs with preserved data. Gradle build and lint pass
(0 errors, 19 warnings). Device `mode remote-buttons`: **OK (16 checks)**,
including all three mappings, installed launch intents and unrelated key handling.
Reconnected the accessibility service after instrumentation, preserving other
enabled services. Physical YouTube button confirmation is pending; Netflix and
Amazon were confirmed by the owner before this update.
APK: `artifacts/living-room-tv-0.9.6-home-debug.apk`.

---

## Branded remote keys — 24/10/2026, version 0.9.5

Installed Home APK and test APK with preserved data. Xiaomi's actual installed
`Vendor_2717_Product_32b9.kl` identifies Netflix as BUTTON_6 (scan codes 240/416)
and Prime Video as BUTTON_7 (207). Added foreground handling plus a protected
AccessibilityService for global handling, mapping Netflix to Netflix and Prime
Video to Jellyfin. All other key codes pass through. The service does not request
window-content access. A link to its Android accessibility settings is in Options.

Gradle APK/test assembly and lint pass (0 errors, 19 warnings). Targeted device
instrumentation: **OK (14 checks)** for both mappings, installed app launch-intent
resolution, unrelated keys and declared key-filter-only capabilities. An earlier
test incorrectly expected the target package's service to remain bound during
instrumentation (which kills that process); corrected the check to inspect the
installed service and verified actual binding separately after instrumentation.
Service is enabled, bound and has no crashed-service entry. Previous enabled
services setting was `null`, saved to
`artifacts/remote-buttons-accessibility-before.txt`.

The owner confirmed both physical buttons: Amazon opens Jellyfin and Netflix
opens Netflix. ADB `input keyevent` reaches Xiaomi's GlobalKeyReceiver
and does not exercise the accessibility input filter, so it is not treated as
proof of physical-button behaviour. APK:
`artifacts/living-room-tv-0.9.5-home-debug.apk`.

---

## Disneyteca supplied background — 24/10/2026, version 0.9.4

Applied `fundo_disneyteca.png` unchanged; source and installed asset SHA-256 match
`edb31459f1faf933aff807f3fcabe233b54db7b41c2eba245c6f59b681e11cb5`.
The twelve covers are now 180×280 scene units in two rows of six, following the
new upper shelf's slope and the lower shelf's baseline. Movie order and remote
navigation are preserved. Previous background and provenance are backed up in
`artifacts/library-disneyteca-before-background.*`.

Gradle Home APK, test APK and lint passed (0 errors, 19 warnings). Installed with
preserved data. Device `mode disney`: **OK (228 checks)**, all twelve live movie
identities and posters confirmed. The diagnostic run also draws the live Disney
view to a PNG for visual verification of cover alignment. App-rendered preview:
`artifacts/disneyteca-background-0.9.4.png`. Restarted the launcher through its Home
alias after testing. APK: `artifacts/living-room-tv-0.9.4-home-debug.apk`.

---

## Netflix view isolation and fallback — 24/10/2026, version 0.9.3

Installed the Home APK and tests with preserved data. Gradle compilation, APK
assembly and lint pass (0 errors, 19 warnings). Device suite: **OK (215 checks)**.
Confirmed title ownership is separate from the profile currently active in
Netflix. Unknown titles need explicit assignment; opposite-view titles are
excluded from the picker. A slot selects confirmed live Netflix, then its own
persistent cached record, then preserves the second Jellyfin ongoing item from
the same view. Cached records include original artwork URL, position, duration,
intent and observation timestamp. Title changes cannot restore the old selection.

Fixtures use isolated preferences and test mixed-profile feeds, empty feeds,
cache recreation, changed choices, corrupt cache, live progress updates and
rejection of opposite-view selections. Live records also pass simulated empty-feed
restoration checks. Provider data and Netflix profiles were not modified by these
tests. On-device cache file persistence is verified.

Live shelves: adults retain Eat Pray Bark / Everwood; kids use Goat and fall back
to Jellyfin's As Três Irmãs for the unavailable Netflix series. Evidence:
`artifacts/netflix-fallback-shelves-0.9.3.txt`.
APK: `artifacts/living-room-tv-0.9.3-home-debug.apk`.

---

## Public Netflix posters — 24/10/2026, version 0.9.2

Replaced the default Netflix shared-card art with portrait posters resolved from
Cinemeta's public catalog API. Matching requires exact title and content type;
ambiguous titles fall back to the shared image. Running Point uses the verified
IMDb identity tt14879018 to distinguish its homonym. Images are cached locally
(24 entries), lookups expire after seven days (15 minutes for no match), and
network requests and response sizes are bounded. Provider metadata, progress and
playback intents remain unchanged. Shared artwork uses fit scaling to keep the
entire image visible instead of clipping faces and titles.

Gradle Home APK, test APK and lint passed: 0 errors, 19 warnings. Both APKs installed
with preserved data. Live `mode posters`: **OK (33 checks)**, including public
portrait downloads for Eat Pray Bark, Everwood and Running Point and rejection of
wrong-type, fuzzy and ambiguous matches. Device smoke suite: **OK (196 checks)**.
The final adults screenshot confirms full, readable Eat Pray Bark and Everwood
posters: `artifacts/adults-public-posters-0.9.2.png`.
APK: `artifacts/living-room-tv-0.9.2-home-debug.apk`.

---

## Netflix selection — 24/10/2026

After the owner opened adult profile and confirmed Everwood, Eat Pray Bark and Running
Point: all three appeared as accessible Netflix Watch Next records. The adults
snapshot loads real `Eat Pray Bark` and `Everwood` records in its reserved Netflix
movie/series positions. `Running Point` is available for selection as the series
instead. The owner identified `Goat` as kids content; it is not used in adults.
Device smoke checks now pass **OK (192 checks)**. The records report positions of
1,693,000 / 5,497,000 ms for Eat Pray Bark, 0 / 2,579,000 ms for the next Everwood
episode, and 1,138,000 / 1,539,000 ms for Running Point. Zero position for Everwood
comes from Netflix, rather than an invented resume point. Local evidence:
`artifacts/netflix-shelves-validation.txt` and
`artifacts/netflix-watch-next-validation.json`.

After restart with network enabled: Gradle Home APK, test APK and lint all pass
(0 lint errors, 19 warnings). Installed both APKs using `install -r`, preserving
the existing session and Home configuration. Device smoke instrumentation passed
**OK (189 checks)**, including the new Netflix selection model checks. An initial
run failed waiting for activity input focus; rerunning with the room foreground
passed. Netflix's live shared feed currently contains only `Goat`, with no profile
identifier and no shared series. Assignment to adult profile still needs
confirmation; no titles have been assigned speculatively. The installed production
APK is copied to `artifacts/living-room-tv-netflix-home-debug.apk`.

The following records describe the earlier restricted session before restart:

Adults and children now have separate saved Netflix film/series title selections
under Options → Netflix for this view. Missing Netflix slots open the selector.
Selections use only real accessible unfinished Netflix Watch Next records; no
profile attribution is inferred from the shared feed. Historical title defaults
remain until a selection is saved. The scan now considers up to 500 provider rows
instead of stopping after 12 usable records.

Production and instrumentation Java sources compile against SDK 36 with Java 17.
Gradle/lint could not start because FileLockContentionHandler could not determine
a usable wildcard IP in this restricted session. A complete Home APK was built
directly with AAPT2, javac, D8, zipalign and apksigner:
`artifacts/living-room-tv-netflix-home-debug.apk`. APK signatures verify and the
certificate matches version 0.9.1. Manifest inspection confirms Home is enabled.

Added regression checks for separate view/slot keys, configured title matching,
preserved provider metadata and missing-title placeholders. These checks compile
but were not run on a device. ADB cannot start its socket listener in this session
(Operation not permitted), so the APK is **not installed**, the requested profiles
were **not accessed**, and the current adult Netflix titles remain unverified.

---

## Complete Disney Library catalogue — 23/10/2026

After Beauty and the Beast (1991) was added to Jellyfin, live `mode disney`
instrumentation passed: **OK (198 checks)**. All **12/12 films** were found in the
Kids libraries with distinct identities and all twelve posters decoded. No code
change or reinstall was required. The app was reopened on the device after testing
to refresh its contents.

---

## Disney catalogue matching — 23/10/2026, version 0.9.1

Installed the Home update over ADB with data preserved. Built directly with SDK 36
and verified certificates. Device `mode disney-corrections` passed **186 checks**,
including real Sleeping Beauty/Snow White matching, the exact `.ia` filename
exception, 1937/1938 release-year handling and remake exclusion. Reopened the app
after testing.

---

## Device installation — 23/10/2026, version 0.9

Installed over ADB at `YOUR_TV_IP:5555` with `install -r`, preserving app data and
the Home configuration. The signing certificate matched the previous APK. Home
still resolved to `com.xsoulp.livingroom/.Home`.

Gradle remained blocked while starting its lock service. Home and test APKs were
built directly with javac 17, AAPT2, D8, zipalign and apksigner from SDK 36.
Artifacts: `artifacts/living-room-tv-0.9-home-debug.apk` and
`artifacts/living-room-tv-0.9-tests.apk`. Signatures were checked before installation.
Gradle Lint was not run.

Live device instrumentation passed **177 checks**, covering four rooms,
four/six-column transitions, larger covers, hidden apps in Books & Stories and
Disney Library, and access to Options. No AndroidRuntime error appeared in the
last 500 log records. The app was reopened after testing. Capture:
`artifacts/room-0.9-installed.png`.

Jellyfin matched 9/12 Disney films. Sleeping Beauty (1959), Beauty and the Beast
(1991), and Snow White and the Seven Dwarfs (1937) were not found in the selected
Kids libraries; those positions kept their names and unavailable state. This did
not yet distinguish missing files from mismatched title/year metadata.

---

## Backgrounds without apps — 23/10/2026, version 0.9

Inspected the three new backgrounds: Books & Stories has eight books, Disney
Library has twelve, and both omit app logos. Kids has the video-library sign,
with its apps and portrait preserved. Native covers were aligned with the book
fronts in the edited images. Instrumentation covers hidden apps, larger covers,
book/Options navigation and room transitions.

Java 17 production and instrumentation compilation against SDK 36 passed.
Gradle/APK/Lint remained blocked at startup by `Could not determine a usable
wildcard IP for this machine`. No installation or device test was performed.

---

## Disney Media — 23/10/2026

- All production and instrumentation classes compiled with Java 17 against Android
  SDK 36; MainActivity's existing deprecated API warning remained.
- All 41 JVM catalogue checks passed, including twelve-film order, Portuguese and
  English aliases, original titles, accents, punctuation, remakes and sequels.
- Visual inspection of `library-disney.png` confirmed two rows of six books, the
  framed portrait and preserved composition. Edited with built-in imagegen; prompt
  recorded in `app/src/main/assets/library-disney.provenance.json`.
- Instrumentation was updated for four rooms and four/six-column transitions.
  `-e mode disney` checks twelve real items and decodes their posters, saving
  `disney-validation.txt` in private app storage.
- APK/Lint builds were not run: Gradle failed to start its lock service with
  `Could not determine a usable wildcard IP for this machine`.
- ADB could not start its local listener (`Operation not permitted`). No installation,
  device navigation test or confirmation of all twelve server films was performed.
  Older APKs in `artifacts/` do not represent this version.

API contract: [Jellyfin GetItems](https://typescript-sdk.jellyfin.org/interfaces/generated-client.LibraryApiGetItemsRequest.html).

---

## Applied backgrounds and cleanup — 21/10/2026

Installed the Home APK successfully at `YOUR_TV_IP:5555`. Confirmed that all three
packaged PNGs matched the current assets byte for byte. Device `mode=stories`
regression passed **136 checks**, including navigation across all three rooms
and Stremio visible only in Adults.

Removed nine obsolete images: two original root backgrounds and seven old captures
or intermediate files in `artifacts/`. Removed two obsolete provenance records
whose images no longer existed. Retained the three active backgrounds, their
provenance records and private source photographs `couple.jpg` and `kids.jpg`.
Reopened the updated launcher after testing. Personal photographs were subsequently
excluded from the public repository during its privacy review.

---

## Kids room portrait — 20/10/2026

Edited `library-kids-portrait.png` from `library-kids.png` and private `kids.jpg`
with imagegen. Visual review confirmed the framed portrait with a light mat on
the right wall above the sign; book and app positions were preserved. Kids loads
the new background, while Books & Stories retains `library-kids.png`.
Home build and Lint passed (`assembleDebug lintDebug -PhomeEnabled=true`).
Updated APK: `artifacts/living-room-tv-home-debug.apk`; not installed on the device.
Exact prompt recorded in `app/src/main/assets/library-kids-portrait.provenance.json`.

---

## Version 0.7 — Stremio only in Adults — 20/10/2026

Home APK, test APK and Lint passed (`assembleDebug assembleDebugAndroidTest lintDebug`).
The Adults background includes the private `couple.jpg` photo in a light-matted
frame. All four app bases fit inside the table; selection targets and covers were
realigned with the final background. The other two rooms retain their previous
backgrounds. Regression checks were extended for Stremio visibility in three rooms,
Netflix/Stremio navigation, shelf access and edge transitions. The tests compiled
but had not yet run on the device for this version. The installation approval
request was declined, leaving the installed version unchanged. ADB confirmed that
`com.stremio.one` was installed.

---

## Version 0.6 — Books & Stories — 20/10/2026

Home APK, test APK and Lint passed. Installed on the development device.
`mode=stories` instrumentation passed **128 checks**, including eight distinct
real items, eight decoded posters and horizontal/vertical navigation across three
rooms, preserving rows and entering at the opposite edge.

Room order cycles through Books & Stories → Kids → Adults. Random selection uses
`sortBy=Random`, `filters=IsNotFolder`, `recursive=true`, `limit=8` and the exact
`Livros e historias` library ParentId, with accent normalization. The observed
sample included Splish Splash Splosh and several Portuguese-language children's
books, including `O Livro com Sono`, `Encontra o Bolinha na Praia`,
`Animais Mágicos da Selva` and `O pinguim que aprendeu a ficar calmo`.

Removed cover strips, overlaid book/app titles and the detail panel in all three
rooms. Retained real progress bars, selection outlines and accessibility names.

Reference: [Official Jellyfin SDK GetItems](https://typescript-sdk.jellyfin.org/interfaces/generated-client.LibraryApiGetItemsRequest.html).

---

## Horizontal edge navigation fix — 20/10/2026

Installed the fix allowing room changes at both left and right edges of the apps
and both shelves. Entry is at the opposite edge with the selected row preserved.
Home build and Lint passed.

Device regression passed **91 checks**. Navigation events are dispatched to the
activity under test to avoid focus contention between MainActivity and the Home
alias. Left-edge switching was also confirmed with `adb shell input keyevent`.
The Options-window check was removed from this stress sequence because Home
removed its window during instrumentation. Room navigation was checked in both
directions from both rooms, for apps and both book rows.

---

## Version 0.5 validation — 20/10/2026

Home `assembleDebug assembleDebugAndroidTest lintDebug` passed. Xiaomi Android 11
instrumentation passed **59 checks**, covering eight positions per room,
ongoing/latest separation, two movie and two series positions per shelf,
Netflix/Jellyfin placement, vertical navigation, immediate room changes at app/book
edges and access to Options. The final visual adjustment enlarged the title area
to separate two title lines from the source name.

The existing Jellyfin session returned `Movies (kids)`, `Shows (kids)`,
`Movies (18+)` and `Shows (18+)`, which the launcher recognises automatically.
The user confirmed Netflix ongoing-title assignments: Adults uses Eat Pray Bark /
Everwood; Kids uses The Loud House Movie / The Loud House. Covers, links and
progress come from real Watch Next records without inferred profile attribution.

A probe under the launcher's UID found four Netflix `watch_next_program` rows,
all `watch_next_type=0`, and no visible Netflix channel or `preview_program`.
Latest additions and profile names could not be obtained from the Netflix UI:
it exposed no accessible text and refused screen capture. The lower shelf keeps
Jellyfin additions. All four records had `type=3`, including movies, so that column
cannot classify movies versus series for this integration.

Public contract: [Android Watch Next](https://developer.android.com/reference/android/media/tv/TvContract.WatchNextPrograms).
`WATCH_NEXT_TYPE_NEW` means a new episode of a followed series, not a general list
of content newly added to Netflix.

---

## Large-book library — 0.4

The television and all television rendering were removed. The new background is
`app/src/main/assets/cozy-library.png`, edited with the built-in imagegen tool;
the exact prompt and source are saved in `cozy-library.provenance.json`.

Eight visible positions replace the previous five: four Netflix Watch Next titles
above and four unfinished Jellyfin movies below. Covers grew from 110–133×139–150
to 244×264 / 244×255 scene units; title text grew from 13 to 26 units. This is
approximately double the linear dimensions, not merely the area. Real data and
explicit app shortcuts are retained. A separate large panel below the shelves
shows the full selection title and opening action. Icon-only app circles remain.

Build and Android Lint passed. Live instrumentation passed **62 checks**, including
eight positions, Netflix/Jellyfin ownership, movies rather than episodes, larger
cover dimensions, selection metadata, D-pad Options, missing apps, and actual API
records/poster decoding. The real device screenshot
`artifacts/cozy-large-library.png` was inspected: no TV, eight real covers loaded,
large selection panel and circular app icons visible. Earlier test counts below
apply to older layouts and remain historical evidence.

---

## Grouped shelves and icon-only apps

The latest update reserves exactly five visible positions: two Netflix Watch Next
Continue titles on the upper shelf and three unfinished Jellyfin **movies** on the
lower shelf. Real records are used; unavailable positions remain explicit app
shortcuts rather than invented films. Book covers have a shaded spine.

The app targets now show the installed SmartTube, Jellyfin and Netflix icons in
circles, with a blue selection ring and no visible app-label or action text.
Accessible names and missing-app handling remain available.

Build and Android Lint passed. Live device instrumentation passed **56 checks**,
including shelf ownership/order, films rather than episodes in the lower row,
TV selection preview, latest-media/fallback behaviour, D-pad Options and missing
apps. This supersedes the earlier mixed shelf layout described below.

---

## Cozy visual baseline — version 0.3

The source reference is `Cozy Smart TV Living Room Launcher.png`. The production
plate is `app/src/main/assets/cozy-room.png`, edited non-destructively with the
built-in imagegen tool. The prompt is saved alongside it in
`cozy-room.provenance.json`. Mock posters, brand tiles, clock, temperature, and
weather were removed; the mountain/lake landscape remains as the actual fallback.

The room now uses a native canvas compositor with perspective TV content and
subtle image-based camera transitions. It replaces the previous low-poly renderer.
Five real shelf cards appear on each bookcase page; selecting an item moves its
actual metadata and progress into the TV. Selecting Jellyfin requests at most two
latest additions, separate from unfinished history. Selecting Netflix or SmartTube
preserves the landscape because no verified latest-additions feed is implemented.

The Home-enabled APK and ordinary APK built successfully. Android Lint passed
with zero errors and 18 warnings. Version 0.3 was installed on the Xiaomi box
without clearing the existing account or changing the Home choice.

Live instrumentation passed **47 checks**, including real authenticated unfinished
items/poster decoding, two real latest additions and their artwork, TV metadata
following the exact selected shelf item, Jellyfin latest-or-landscape behaviour,
Netflix and SmartTube landscape fallback, D-pad navigation, Options without Menu,
and missing-app handling. The real latest additions returned were Builder Brothers'
Dream Factory and Morangos com Açúcar, with both posters decoded. These titles are
observations from this account, not production fixtures. The metadata-only evidence
is in `artifacts/latest-validation.json` (ignored by Git).

Actual ADB screenshots were inspected:

- `artifacts/cozy-jellyfin-latest.png`: two genuine additions inside the room's TV.
- `artifacts/cozy-shelf-details.png`: selected movie artwork, real description,
  progress, and native Resume guidance inside the TV.
- `artifacts/cozy-landscape.png`: Netflix selected with the mountain/lake fallback.

Home still resolves to `com.xsoulp.livingroom/.Home`. No AndroidRuntime crash was
observed in the final log check. APK checksums, sizes, and Home-alias states are in
`artifacts/cozy-build-validation.json`. The new layout preserves the previously
verified Jellyfin item-opening code; the earlier 12-item playback results below
belong to version 0.2, not a fresh full playback run of 0.3.

---

## Device validation

Date: 2026-10-19. Device: Xiaomi MiTV-AFKR0 / jaws, Android 11 API 30,
32-bit ARM, 1920×1080 display. Package: `com.xsoulp.livingroom`, version 0.2.

### Build and static checks

- Home-enabled debug APK, test APK, and Android Lint built successfully with
  Java 21, Gradle 9.6.0, AGP 9.4.0, and Android SDK 36.
- Lint: zero errors, 20 warnings. These include prototype layout/text, orientation,
  backup configuration, and newer target SDK notices; see
  `app/build/reports/lint-results-debug.html`.
- Ordinary APK also builds successfully, with its Home alias disabled.
- `bash -n build.sh install.sh restore-google-tv.sh` passed.
- Both APK variants are debug signed. Release signing and store distribution are
  outside this prototype's validation.

### Real data and remote navigation

Live instrumentation passed 33 checks. It read 12 genuinely unfinished Jellyfin
movies/episodes and decoded all 12 posters using the launcher's authenticated
session. No production shelf entries came from test fixtures.

The same checks verified tick conversion, progress, completed/unstarted filtering,
episode labels, bounded API responses, server URL validation, Watch Next owner
filtering, D-pad shelf entry/exit, Options through Down then OK, horizontal Options
selection, Menu access, and a missing-app dialog. Missing-app simulation only
replaced the candidate package array inside the test process; it did not disable
or uninstall media apps.

The final live test was repeated after the playback change and again passed all
33 checks. Manual D-pad navigation from the actual room opened SmartTube's browse
activity and Netflix's main activity. Up then OK on a real shelf poster opened the
matching Jellyfin episode details with its native Resume button visible.

The ordinary TV listings permission exposed 27 Watch Next rows on this firmware;
three real unfinished Netflix records passed the filters. No accessible SmartTube
Continue records were available; its shelf entry opens SmartTube. This result is
specific to the installed provider, firmware, apps, and account.

`artifacts/room-current.png` is an actual ADB screen capture showing the room,
loaded posters, progress bars, app selection, and the Down-to-Options hint.

### Jellyfin playback

All 12 real unfinished items were opened and successfully resumed near their saved
positions with advancing playback confirmed by the server, across a full run and
one targeted retry. The full run passed 11/12; one Richard Scarry episode initially
reported a position moving backwards instead of advancing from its saved 123-second
position. Retesting that episode alone passed at the same saved position. This
transient failure remains recorded rather than being hidden by the successful retry.

The tests now return to Home before each item and allow Jellyfin's session teardown
to settle, matching normal launcher use. Earlier tests started successive item links
while still inside Jellyfin and encountered navigation resets. The production intent
uses `NEW_TASK` without clearing the client's task.

Detailed local results: `artifacts/playback-validation.json` and
`artifacts/playback-retry-validation.json`. These contain viewing titles and positions
but no credentials, and are ignored by Git.

The test uses real resume records, opens Jellyfin's exported item entry point,
selects its native Resume control, and checks the server's Sessions API for the
same user and item, a position near the saved position, and advancing playback.
It pauses playback after each item. This briefly changes real viewing positions.
Detailed results are saved in the launcher's private `playback-validation.json`.

The production launcher opens item details; the viewer selects Resume in Jellyfin.
It does not automatically click controls or use the instrumentation in production.

### Default Home and recovery

The Home role and resolved Home activity were verified as
`com.xsoulp.livingroom/.Home`. The Home key brought the launcher to the foreground.
The owner explicitly authorized this switch. Google TV launcher and setup-wizard
packages remain installed but are disabled for user 0 to avoid their higher
priority Home handlers on this firmware.

Opening Android settings was verified with only D-pad keys: Home, Down, OK,
six Down presses, OK. The foreground activity became
`com.android.tv.settings/.MainSettings`. No Menu key or long press was required.

`restore-google-tv.sh` restores both stock packages and the Home role/preference.
It has passed shell syntax validation but has not been executed, because doing so
would undo the requested default launcher. Use Home-enabled APK updates until
stock Home is restored.

### Limits

Frame rate, long-duration thermal behaviour, sleep/wake endurance, and offline
recovery have not been systematically measured. The 720p/540p modes, simple
geometry, static shading, small textures, and idle rendering are performance
choices rather than benchmark claims. Netflix and SmartTube playback/resume
behaviour depends on their own available item intents and login state.

### Public README previews and release packaging (2026-11-07)

- Added browser-rendered Kids top/middle shelf views and a selected Brave cover.
- Day/night browser screensaver captures contain visible illustrative easyJet and
  Ryanair aircraft. Preview labels distinguish these from native/live captures.
- Browser room regression checks passed (78 checks). No Android runtime changes.
- README images use 360 px display widths linked to full-resolution WebP files.
  Seven images decreased from 19,146,690 to 2,933,442 bytes (84.7% smaller);
  each individual image is at least 50% smaller than its PNG source.
- Tagged public builds publish verified APKs/checksums as GitHub Release assets
  after successful builds. Hosted release execution is not locally verified.

### Public source cleanup and history reset (2026-11-07)

- Reviewed all 21 pre-publication commits and 330 unique file blobs. Credential,
  personal-path/name/email and PNG-metadata searches found only synthetic test
  examples; all author/committer identities use the public GitHub handle.
- Removed 25 unused/duplicate legacy assets and captures; retained a compact WebP
  gallery and the two browser preview sources. No runtime image lookup references
  the removed assets. Updated the current Kids room provenance/hash.
- Added MIT-0 and a voluntary acknowledgment request, with explicit third-party
  exclusions. Markdown links, browser JS, workflow YAML and shell syntax pass.
- All 99 JVM tests and 78 browser preview checks pass. Both public APK variants
  build and pass Android lint. Package checks exclude private inputs and unused
  legacy assets, verify the public signing key and check the Home manifest flag.
- The owner requested one clean publication commit and no earlier Git history.
  The version tag is rewritten to the same clean snapshot. Local originals and a
  history backup remain ignored and are not published.
- Hosted Actions/release availability and repository visibility are separate from
  these local checks; this audit does not claim a completed hosted build.
