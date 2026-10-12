# Release and build guide

`VERSION` is the single application version source. Official releases use a tag matching it exactly, without a `v` prefix. Beta tags such as `2.0.0b5` and release-candidate tags such as `2.0.0rc1` become GitHub prereleases whose title is the version only.

## Choose a route

| Route | What it produces | Where it runs |
| --- | --- | --- |
| Full release (stable, beta, or release candidate) | Production-signed universal, arm64-v8a, and x86_64 Android APKs; x86_64 and AArch64 Linux bundles; checksums; and a version-titled GitHub Release | Local Linux host through `tools/release.sh`; Windows ZIP is attached afterward by Actions |
| Optional remote builds | Android debug APK and/or Linux AArch64 bundle as temporary workflow artifacts | Manually dispatched Actions workflow; not a production release |
| Entirely Actions-based signed release | Not currently supported | No workflow currently handles Android production signing and creates the complete release |

## Current release status

The public GitHub release state was verified on 2026-10-12. Stable `2.0.1` is published ([release page](https://github.com/hanaretamae/Kaede-Gallery/releases/tag/2.0.1)) with universal, arm64-v8a, and x86_64 Android APKs; x86_64 and AArch64 Linux `.tar.zst` and `.AppImage` bundles; `SHA256SUMS`; and license materials. At the last check, the Windows x64/ARM64 release workflow [38239334210](https://github.com/hanaretamae/Kaede-Gallery/actions/runs/38239334210) was still running, so Windows archives were not yet confirmed on the release page. Refresh the release page before stating which Windows assets are available. Stable `2.0.0` is also published with Android, Linux x86_64/AArch64, and Windows x64/ARM64 assets ([release page](https://github.com/hanaretamae/Kaede-Gallery/releases/tag/2.0.0)).

The older `2.0.0b4` prerelease has no Windows ZIP: workflow run [38136147012](https://github.com/hanaretamae/Kaede-Gallery/actions/runs/38136147012) failed in `LinuxPortalDirectoryPickerTest.resolvesPortalImportAndExportFilesWithoutAcceptingNonLocalUris`, so its archive and upload steps were skipped. Phase 14 was subsequently closed by explicit owner approval using the stable release assets and successful Windows workflow run [38199667594](https://github.com/hanaretamae/Kaede-Gallery/actions/runs/38199667594); the missing b4 artifact remains a waived historical omission, not a passed upload. Do not move or recreate historical tags.

Android version codes reserve suffixes b1-b898 for beta builds, rc1-rc100 for release candidates, and 999 for the final release. The release script permits Markdown-only documentation commits after the tag, except changes to `CHANGELOG.md`; it requires the current commit to match `origin/main`. Keep the release notes and all non-documentation release code at the tagged commit.

## 2.0.1 published release

Stable `2.0.1` was published on 2026-10-12 as a non-prerelease GitHub Release. The release page contains the three Android APK variants, Linux x86_64/AArch64 `.tar.zst` and AppImage bundles, checksums, and license materials. Starting with 2.0.1, Linux bundles include pinned `mpv` and `ffmpeg` executables with their runtime closure; unlike the 2.0.0 Linux downloads, they do not require those executables from the host `PATH`. The Windows x64/ARM64 archives are produced by the separate workflow and must be verified on the public page before claiming they are attached. Physical Windows runtime behavior and untested hardware/provider combinations remain unverified.

## Full release from a local Linux host

For a future, not-yet-published release only, use a clean checkout of the reviewed `main` commit and confirm `VERSION` matches the intended release tag. `2.0.1` is already published; do not rerun these commands for it. The release script verifies the tag, branch, GitHub authentication, test prerequisites, signing files, Rust targets, and Nix builders before publishing.

```sh
git switch main
git pull --ff-only origin main
git fetch origin --tags
cat VERSION
git status --short
```

Configure paths to the KeePassXC database and key file locally. Do not place their contents in the repository, command arguments, or chat.

```sh
export KEEPASSXC_DATABASE="/absolute/path/to/signing-database.kdbx"
export KEEPASSXC_KEY_FILE="/absolute/path/to/signing-key-file"
release_tag="$(cat VERSION)"
tools/release.sh "$release_tag"
```

The expected KeePassXC entry defaults to `Kaede Gallery Android signing`, with attachment `release.jks`. The script exports the key temporarily, builds and verifies the signed universal, arm64-v8a, and x86_64 APKs, runs the Rust and Kotlin tests, creates x86_64 and AArch64 Linux bundles as `.tar.zst` archives and `.AppImage` files (a pinned `appimagetool` and type2 runtime from the flake wrap the Nix bundle), calculates checksums, and publishes a version-titled stable release or prerelease according to the tag. Starting with 2.0.1, each Linux bundle is a self-contained Nix closure including pinned `mpv`, `ffmpeg`, runtime libraries, and license materials; the release script checks that both media tools are present in each architecture's package closure before bundling. It requires Rustup 1.98.1 with `aarch64-linux-android` and `x86_64-linux-android` targets already installed. Both Linux bundles are built on an x86_64 Linux builder; the AArch64 Rust FFI is cross-compiled and target-native runtime files are assembled without QEMU/binfmt or an AArch64/remote builder. `XDG_RUNTIME_DIR` must be a private directory owned by the current user with mode `700`; signing material is kept there. Large temporary release artifacts are staged in a private directory under `TMPDIR` (default `/tmp`) to avoid filling small runtime tmpfs mounts.

After creating a future GitHub Release, build and attach its Windows ZIP through the manual Windows workflow:

```sh
gh workflow run windows.yml \
  --repo hanaretamae/Kaede-Gallery \
  --ref main \
  --field tag="$(cat VERSION)"
gh run list --repo hanaretamae/Kaede-Gallery --workflow windows.yml
```

The workflow verifies the tag and version, packages Windows x64, and uploads the ZIP and updated checksums to the existing release. It cannot create the release itself. Both Windows workflows package `Kaede-Gallery.exe` with `LICENSE`, its Japanese reference translation, English and Japanese READMEs, and English and Japanese third-party notices beside the launcher; verify these files in the artifact before distribution.

## Optional remote builds

For artifact-only Android/Linux ARM builds, enter the Nix development shell and use the CLI wrapper. The default ref is `main`; the optional second argument selects another branch or tag containing the workflow.

```sh
nix develop
tools/run-optional-builds.sh android-debug
tools/run-optional-builds.sh linux-aarch64
tools/run-optional-builds.sh both
```

Choose one target per invocation. The workflow also appears in the GitHub Actions UI as **Optional Android and Linux AArch64 builds**. It is manual-only, keeps local builds as the normal path, and uploads artifacts for 14 days. `android-debug` is a debug-signed APK, not the production APK; `linux-aarch64` cross-builds an AArch64 bundle on an x86_64 runner. This workflow does not build the x86_64 release bundle, create a GitHub Release, or sign/publish a production Android APK.

The automatic `kotlin-windows.yml` artifact and the manual `windows.yml` ZIP both include checksum-verified Windows mpv/FFmpeg builds under `tools/` and their license texts under `licenses/`. The manual workflow can produce an artifact without a tag, but it only attaches a ZIP when dispatched with a tag for an already-created GitHub Release.

## Entirely Actions-based production release

A complete Actions-only signed release is **not available yet**. The current production signing process reads the keystore and password from the release owner's local KeePassXC database. The optional build workflow only creates temporary debug/build artifacts, and the Windows workflow requires a release to exist first.

Supporting a full remote release would require a dedicated protected release workflow, a deliberate decision to provide the Android signing keystore/password through a protected GitHub Environment or an external secret manager, and CI verification of the signed APKs and all release bundles. Do not upload the KeePassXC database/key file or put signing material in repository files. This remote signing setup has not been approved or configured. The 2.0.1 release is already published; use the local release procedure above only for a future, approved version.
