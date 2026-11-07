# Ambient music

Family Shelf bundles two recordings downloaded directly from Incompetech:

| Room | Track | Artist | Duration |
| --- | --- | --- | --- |
| All six Kids shelves | Music Box Theme | Kevin MacLeod | About 1:04 |
| Adults | Darkest Child | Kevin MacLeod | About 3:59 |

Incompetech offers free use with attribution under CC BY 4.0. Credits appear in
**Options → Help / controls** and in [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md).
The [source manifest](music-sources.json) records the original URLs and SHA-256
checksums. The bundled MP3s are the original downloads, unchanged. No generated
music or composition generator is included.

Music fades in over 650 ms and plays at 13% of the media stream gain. The TV's
media volume still controls final loudness. **Options → Background music** saves
an on/off preference. The Kids track does not restart when moving between stories,
kids media and Disney. Changing to Adults selects its suspense track.

Playback requires a resumed launcher, an interactive screen and granted Android
audio focus. Opening another app, leaving the launcher, switching the screen off
or muting music stops and releases the player. Temporary focus loss pauses it;
permanent loss keeps it silent until a later foreground entry or explicit re-enable.
The launcher does not interrupt music already playing in another app.

The browser prototype starts music only after a click on **Music off**. **Adults**
switches both the example room and the track. It uses the same downloaded files.
Local state-machine tests cover foreground changes, mute, track changes, denied
focus and temporary/permanent interruptions. Actual TV volume and focus callbacks
remain to be verified after installation.

Sources: [Incompetech free licensing](https://incompetech.com/music/royalty-free/licenses/),
[Music Box Theme](https://incompetech.com/music/royalty-free/index.html?isrc=USUAN1100417),
[Darkest Child](https://incompetech.com/music/royalty-free/index.html?isrc=USUAN1100783).
Android reference: [Manage audio focus](https://developer.android.com/media/optimize/audio-focus).
