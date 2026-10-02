# Flutter アプリ

このアプリは Phase 3 の Linux 向けギャラリー UI とビューアーです。リポジトリのルートから
`nix develop` に入り、次を実行してください。

```sh
cd app
flutter pub get
GDK_BACKEND=wayland flutter run -d linux
```

最初の Rust ネイティブアセットビルドは、ルートの `rust-toolchain.toml` に固定された
バージョンを使用し、Rustup 経由でそのツールチェインをダウンロードすることがあります。
アプリは、索引とサムネイルを選択した Vault の外、アプリケーションサポートディレクトリに保存します。
