# Public repository privacy review

Reviewed for initial publication on 2026-10-28.

The public source was prepared in a separate checkout from an explicit list of
project files. Original personal photographs and unreviewed device exports were
not copied into that checkout.

- Real family photographs in the active room backgrounds were replaced with
  AI-generated portraits of fictional people. Original photographs remain local.
- Embedded image metadata and thumbnails were removed without changing pixel data.
- Only reviewed example screenshots are included in `docs/examples/`. The adult
  example has the same fictional-portrait treatment; the Disney Library screenshot
  has no personal portrait. Examples may show media artwork and example titles.
- Personal Netflix profile names were replaced with generic adults/kids wording.
- The Jellyfin connection screen no longer supplies a private server address.
- Installation scripts require an explicit TV address or `ANDROID_TV_TARGET`.
- Historical documentation uses generic profile names and address placeholders.
- Device inventories, logs, validation exports, private APKs, personal signing keys, local SDK
  configuration and tool/account directories are excluded from publication.
- Git commit metadata uses the public GitHub handle and a GitHub noreply address.

The public GitHub handle `xsoulp`, Android namespace `com.xsoulp.livingroom`, and
existing technical identifiers remain intentionally visible. They identify the
public project and preserve update compatibility.

Text files were checked for known personal names, private network addresses,
local home paths and common credential formats. Published images were visually
reviewed. These checks describe the reviewed initial snapshot; future screenshots,
exports and commits should be reviewed again before publication.

Family Shelf stores a user's Jellyfin session locally using Android Keystore.
Users supply their own server address and account. The repository contains no
configured user session, server token or media library export.

The fictional portrait edits use the built-in image generation tool. The
[prompts](image-prompts.json) are included for reproducibility; private input
photographs are excluded.

The English-language update also localises room signs and example interface
labels. Original-language library matching aliases and media artwork are retained.

Public APK workflow builds use an empty private-asset directory and check that
personal photo, outdoor soundtrack and location files are absent. The dedicated
`.github/public-debug.keystore` is a newly generated, intentionally public debug
key for consistent CI updates; it is separate from the owner's private signing
identity. Public build details are in [PUBLIC_APK.md](../.github/PUBLIC_APK.md).

## Public release review — 2026-11-07

Before replacing the development history, all 21 reachable commits and 330 unique
file blobs were inspected for private paths, addresses, personal names, email
addresses, credential formats and embedded PNG metadata. Matches were limited
to synthetic test fixtures; commit authors and committers used the public handle
and GitHub noreply address. No private asset, APK, account export or local-tool
folder was found in the reviewed history.

The current public images were visually reviewed. Retained PNG/WebP assets have
no EXIF, text or XMP metadata; original music files retain their artists' tags.
The public development certificate identifies only Family Shelf, not a private
signing identity.

The publication snapshot removes unused legacy room/sprite assets and obsolete
or duplicate screenshots. Originals are archived locally outside the repository.
Current source, build tooling, tests, documentation, provenance, licensed audio
and reviewed examples remain. The public Git history is reset to a single
publication commit at the owner's request, including the version tag.

Original code and documentation are licensed under MIT-0. The README's request
for acknowledgment is voluntary. Third-party material retains its own rights.
The repository visibility itself is not changed by this review.
