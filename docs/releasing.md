# Release and build guide

`VERSION` is the single application version source. Official releases use a tag matching it exactly, without a `v` prefix. Beta tags such as `2.0.0b4` become GitHub prereleases whose title is the version only.

## Choose a route

| Route | What it produces | Where it runs |
| --- | --- | --- |
| Full release (recommended for 2.0.0b4) | Production-signed universal, arm64-v8a, and x86_64 Android APKs; x86_64 and AArch64 Linux bundles; checksums; and a GitHub prerelease | Local Linux host through `tools/release.sh`; Windows ZIP is attached afterward by Actions |
| Optional remote builds | Android debug APK and/or Linux AArch64 bundle as temporary workflow artifacts | Manually dispatched Actions workflow; not a production release |
| Entirely Actions-based signed release | Not currently supported | No workflow currently handles Android production signing and creates the complete release |

## Current 2.0.0b4 status

As checked on 2026-10-11, `2.0.0b3` is the latest published KMP prerelease. The `2.0.0b4` source commit and matching tag exist, but no GitHub Release has been published for that tag. Run the local release script only after verifying the tag and completing the release gates below. The prior `2.0.0b3` and `2.0.0b2` tags are historical; do not move or recreate them.

The release script permits Markdown-only documentation commits after the tag, except changes to `CHANGELOG.md`; it requires the current commit to match `origin/main`. Keep the release notes and all non-documentation release code at the tagged commit.

## Preparing the stable 2.0.0 release

The repository remains on `2.0.0b4`; this guide update does not change `VERSION`, create a tag, or publish anything. Do not begin the stable release until the Phase 14 acceptance gate in `docs/design.md` is satisfied, including the required prerelease and Windows archive checks. Then review and finalize the draft in `CHANGELOG.md` against the release commit, update `VERSION` to `2.0.0`, and include both in a reviewed release commit. Commit and push that exact source, create and push the matching `2.0.0` tag, and run the full local release procedure below with `tools/release.sh 2.0.0`. Unlike beta tags, `2.0.0` is published as a stable release.

Before publishing, verify the generated Android APK signatures and package permissions, the Linux x86_64/AArch64 bundles, generated license notices, checksums, and the Kotlin/Rust test results. Attach the Windows archive only after the stable GitHub Release exists. Record only target checks that actually completed; Windows runtime execution and any untested hardware/provider combinations remain unverified. The exact release assets and checksums should be checked on the public Release page after upload.

## Full release from a local Linux host

Use a clean checkout of the latest `main`, and confirm `VERSION` is `2.0.0b4`. The release script verifies the tag, branch, GitHub authentication, test prerequisites, signing files, Rust targets, and Nix builders before publishing.

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
tools/release.sh 2.0.0b4
```

The expected KeePassXC entry defaults to `Kaede Gallery Android signing`, with attachment `release.jks`. The script exports the key temporarily, builds and verifies the signed universal, arm64-v8a, and x86_64 APKs, runs the Rust and Kotlin tests, creates x86_64 and AArch64 Linux bundles, calculates checksums, and publishes a version-titled prerelease. It requires Rustup 1.98.1 with `aarch64-linux-android` and `x86_64-linux-android` targets already installed. Both Linux bundles are built on an x86_64 Linux builder; the AArch64 Rust FFI is cross-compiled and target-native runtime files are assembled without QEMU/binfmt or an AArch64/remote builder. `XDG_RUNTIME_DIR` must be a private directory owned by the current user with mode `700`.

After the prerelease exists, build and attach the Windows ZIP through the manual Windows workflow:

```sh
gh workflow run windows.yml \
  --repo hanaretamae/Kaede-Gallery \
  --ref main \
  --field tag=2.0.0b4
gh run list --repo hanaretamae/Kaede-Gallery --workflow windows.yml
```

The workflow verifies the tag and version, packages Windows x64, and uploads the ZIP and updated checksums to the existing release. It cannot create the release itself.

## Optional remote builds

For artifact-only Android/Linux ARM builds, enter the Nix development shell and use the CLI wrapper. The default ref is `main`; the optional second argument selects another branch or tag containing the workflow.

```sh
nix develop
tools/run-optional-builds.sh android-debug
tools/run-optional-builds.sh linux-aarch64
tools/run-optional-builds.sh both
```

Choose one target per invocation. The workflow also appears in the GitHub Actions UI as **Optional Android and Linux AArch64 builds**. It is manual-only, keeps local builds as the normal path, and uploads artifacts for 14 days. `android-debug` is a debug-signed APK, not the production APK; `linux-aarch64` cross-builds an AArch64 bundle on an x86_64 runner. This workflow does not build the x86_64 release bundle, create a GitHub Release, or sign/publish a production Android APK.

The separate `windows.yml` workflow can produce a Windows artifact without a tag, but it only attaches a ZIP when dispatched with a tag for an already-created GitHub Release.

## Entirely Actions-based production release

A complete Actions-only signed release is **not available yet**. The current production signing process reads the keystore and password from the release owner's local KeePassXC database. The optional build workflow only creates temporary debug/build artifacts, and the Windows workflow requires a release to exist first.

Supporting a full remote release would require a dedicated protected release workflow, a deliberate decision to provide the Android signing keystore/password through a protected GitHub Environment or an external secret manager, and CI verification of the signed APKs and all release bundles. Do not upload the KeePassXC database/key file or put signing material in repository files. This remote signing setup has not been approved or configured. For `2.0.0b4`, use the local release procedure above.
