# Flutter app

English | [日本語](README.ja.md)

The Flutter app supports Linux, Android, and Windows. For Linux desktop, enter
`nix develop` from the repository root and run:

```sh
cd app
flutter pub get
GDK_BACKEND=wayland flutter run -d linux
```

See the root [README](../README.md#build-an-android-apk) for Android build steps and platform support.

The first Rust native asset build uses the version pinned in the root
`rust-toolchain.toml` and may download that toolchain through Rustup. The app stores the index and thumbnails
outside the selected Vault, in the application support directory.
