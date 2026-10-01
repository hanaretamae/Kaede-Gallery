# Flutter application

This app is the Phase 2 Linux gallery UI. From the repository root, use
`nix develop`, then run:

```sh
cd app
flutter pub get
flutter run -d linux
```

The first Rust native-assets build uses the pinned version in the root
`rust-toolchain.toml` and may download that toolchain through Rustup. The app
keeps its index and thumbnails in its application-support directory, outside
the selected Vault.
