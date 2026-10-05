# Flutter アプリ

<a href="README.md">English</a> | 日本語

この Flutter アプリは Linux と Android に対応しています。Linux デスクトップ版は
リポジトリのルートから `nix develop` に入り、次を実行してください。

```sh
cd app
flutter pub get
GDK_BACKEND=wayland flutter run -d linux
```

Android のビルド手順と対応範囲は、ルートの [README](../README.ja.md#android-apk-のビルドと配布) を参照してください。

最初の Rust ネイティブアセットビルドは、ルートの `rust-toolchain.toml` に固定された
バージョンを使用し、Rustup 経由でそのツールチェインをダウンロードすることがあります。
アプリは、索引とサムネイルを選択した Vault の外、アプリケーションサポートディレクトリに保存します。
