# Public APK builds

The Public APK workflow builds two installable debug APKs from a clean checkout:
`family-shelf-home.apk` (can become Home) and `family-shelf.apk` (regular app).
Both are saved in the `family-shelf-public-apks` workflow artifact with SHA-256
checksums. It runs on pushes to main and can also be started manually in Actions.
Downloading workflow artifacts requires GitHub sign-in and repository access.

The build explicitly points private asset inputs at an empty temporary directory
and verifies that personal photo, soundtrack and weather-location assets are absent.
No private configuration or signing credentials are read from the developer's TV.

`public-debug.keystore` is a newly generated, deliberately public **development**
signing key. Its alias is `androiddebugkey` and both passwords are `android`.
It keeps public debug APK updates compatible across workflow runs. It is not the
owner's personal signing key and must not be treated as a production secret.
Local builds use their normal local key unless `-PpublicDebugSigning=true` is set.
A private installation signed with another key cannot update directly to these
public APKs; retain its original key to preserve that update path.

## Release downloads

Pushing a version tag such as `v0.15.2` runs the same public build and checks,
then publishes both APKs and `SHA256SUMS` as GitHub Release assets. The release
job has repository write permission; ordinary branch builds retain read-only
permissions. Release publishing only runs after the build succeeds.

When the repository is public, release assets can be downloaded without a GitHub
login. Workflow artifact downloads still require signing in. Repository visibility
is managed separately; this workflow does not change it.

Rebuilding an existing version tag replaces its release assets and updates the
release description after checksum verification. Repository history is not
embedded in generated release notes.
