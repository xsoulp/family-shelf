# Third-party credits

## Kids room

"Music Box Theme" by Kevin MacLeod ([incompetech.com](https://incompetech.com/)).
Licensed under [Creative Commons Attribution 4.0 International](https://creativecommons.org/licenses/by/4.0/).
[Original track](https://incompetech.com/music/royalty-free/index.html?isrc=USUAN1100417).

## Adults room

"Darkest Child" by Kevin MacLeod ([incompetech.com](https://incompetech.com/)).
Licensed under [Creative Commons Attribution 4.0 International](https://creativecommons.org/licenses/by/4.0/).
[Original track](https://incompetech.com/music/royalty-free/index.html?isrc=USUAN1100783).

Both bundled MP3 files are the original downloaded recordings, unchanged.
Family Shelf plays them repeatedly at a low volume and applies a playback fade-in.
Source URLs, download date and checksums are in [music-sources.json](docs/music-sources.json).
The music's CC BY 4.0 license applies to these recordings, not automatically to
other files in this project. No endorsement by the artist is implied.

## Outdoor weather

Current model conditions and sunrise/sunset data are supplied by
[Open-Meteo](https://open-meteo.com/), with provider credit shown on the screensaver.
See the [API documentation](https://open-meteo.com/en/docs) and
[terms](https://open-meteo.com/en/terms). Weather readings keep their source timestamp;
old cached data is labelled as saved and is not presented as a fresh observation.

## Live aircraft positions

Aircraft positions are supplied by [ADSB.lol](https://www.adsb.lol/), a community
ADS-B network. Its public API/data are licensed under the
[Open Database License 1.0](https://opendatacommons.org/licenses/odbl/1-0/).
The screensaver credits ADSB.lol and ODbL on screen. Video-overlay credits and
provider/license URLs appear in Options → Video aircraft overlay → About video
aircraft, rather than under each animated aircraft. It queries only nearby aircraft
while visible, derives bridge-corridor crossings and retains only short-lived
in-memory positions for detection and duplicate suppression. No aircraft database
or historical flight dataset is bundled with Family Shelf.

[API documentation](https://api.adsb.lol/docs),
[API usage notes](https://github.com/adsblol/api/blob/main/README.md).
The currently free public service has dynamic limits and no availability guarantee;
Family Shelf backs off after errors and respects numeric Retry-After delays.

## Aircraft pass-by sound

[Airplane, pass #4](https://bigsoundbank.com/airplane-pass-4-s3007.html) by
Dj Lio, downloaded from BigSoundBank under
[CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/).
The full downloaded sound was converted to mono 22,050 Hz PCM16 WAV to fit
Android SoundPool's decoded sample memory. It replaces the previous Air whoosh
and is bundled under the existing resource name `airplane_whoosh.wav`.
Source, download and hashes are recorded in [whoosh-source.json](docs/whoosh-source.json).

## Private outdoor music

The outdoor player can use a user-provided MP3 from ignored local assets. The
owner's supplied Bonito, Bonito recording is not redistributed with the repository
or public APK. Its inclusion in the private build does not change its ownership
or license. The audio player loops the original file and reduces playback volume.

## Hourly church bell

[Bell 1 O'clock](https://bigsoundbank.com/bell-1-o-clock-s3446.html) by Joseph SARDIN
& Axeline T., downloaded from BigSoundBank under
[CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/).
The full recording is converted to mono 22,050 Hz PCM16 WAV for SoundPool and
repeated once per hourly strike. No synthesized audio is included.
[Download details and hashes](docs/church-bell-source.json).

## Artwork, media covers and product names

The illustrated room, cloud and aircraft assets were created with image-generation
tools; prompts and provenance are included in `docs/` and beside the room assets.
Public room portraits depict fictional people.

Movie/book covers in screenshots and browser previews illustrate external library
content. Jellyfin, Netflix, Disney and other product names or logos remain the
property of their respective owners. Their appearance does not imply affiliation
or endorsement, and the project's MIT-0 license does not relicense them.

## Build tooling

The Gradle wrapper is distributed under Apache License 2.0; its source license
header is retained in `gradlew`. Android/Gradle tooling and test dependencies keep
their upstream licenses and are not relicensed under MIT-0.
