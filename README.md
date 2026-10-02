<h1 align="center">Kaede Gallery</h1>

<p align="center">
  タグ付けされた Obsidian Vault のノートを、<b>オフライン・読み取り専用</b>で眺めるギャラリー。<br>
  Rust（パーサー・索引・CLI）と Flutter（Linux / Android UI）で構築しています。
</p>

<p align="center">
  <img alt="Platform" src="https://img.shields.io/badge/platform-Linux-informational">
  <img alt="Rust" src="https://img.shields.io/badge/core-Rust-orange">
  <img alt="Flutter" src="https://img.shields.io/badge/UI-Flutter-02569B">
  <img alt="Offline" src="https://img.shields.io/badge/network-none-success">
</p>

> [!IMPORTANT]
> Vault には**一切書き込みません**。索引・サムネイル・設定はすべて Vault の外のアプリ専用データに保存します。
> ネットワーク通信・テレメトリ・アナリティクスもありません。

## 目次

- [特徴](#特徴)
- [クイックスタート](#クイックスタート)
- [NixOS / Home Manager へのインストール](#nixos--home-manager-へのインストール)
- [Flutter ギャラリーの起動](#flutter-ギャラリーの起動)
- [Android APK のビルドと配布](#android-apk-のビルドと配布)
- [設定の構成](#設定の構成)
- [ノート構造とパーサー](#ノート構造とパーサー)
- [プライバシーとセキュリティ](#プライバシーとセキュリティ)
- [パフォーマンスとテストデータ](#パフォーマンスとテストデータ)
- [リポジトリ構成](#リポジトリ構成)

## 特徴

| 分野 | 内容 |
| --- | --- |
| 閲覧 | ノート単位／全メディアの表示切替。タイルに通し番号・複数画像数・覚書数・関連数を表示可能 |
| ページング | 1ページの件数を設定可能。「総件数 件中 先頭 - 末尾 件」を表示し、任意の位置へジャンプ。スクロールに合わせて前後を追加読み込み |
| 検索 | ノート名、`#タグ`、`-#タグ`、`&#タグ`、タグ名のあいまい検索 |
| 絞り込み | 階層タグを +／−／AND／無選択で絞り込み。同カテゴリ内は OR、カテゴリ間は AND。該当ノートのないタグは暗転 |
| ビューアー | 黒背景でメディアを最大表示。タップで投稿者・投稿文・覚書・関連などの詳細を表示。複数メディアはスワイプ／左右キー／トラックパッドで切替 |
| 関連リンク | Markdown リンクと Wikilink（`![[…]]`・`![](…)` を含む）の指す先がノートなら、アプリ内の詳細ビューで開く |
| 動画 | mpv による再生。再生速度・1本ループ・ミュート。サムネイルは FFmpeg ベース |
| 外観 | Material 3 Expressive を意識した配色・形状・動き、Material You（動的カラー）、システム／ライト／ダーク／ピュアブラック |
| ノート構造 | ブロック順序（投稿者・メディア・投稿文・投稿文の終端・関連・覚書）、見出し名・レベル・箇条書きの有無、Frontmatter キーを設定可能 |

> [!NOTE]
> Flutter は Material 3 Expressive の全コンポーネントを提供していないため、標準の Material 3 部品の範囲で実装しています。
> Android はサイドロード用 APK をビルドでき、SAF で選択した通常フォルダを読み取り専用で走査できます。実機でのDocumentsプロバイダ確認は別途必要です。

## クイックスタート

`nix develop` で開発シェルに入るか、Rust stable と C コンパイラを用意してください。

```sh
nix develop
cargo run --release -p gallery-cli --bin generate-dummy-vault -- /tmp/dummy-vault --notes 100
cargo run --release -p gallery-cli -- scan /tmp/dummy-vault
cargo run --release -p gallery-cli -- categories /tmp/dummy-vault
cargo run --release -p gallery-cli -- list /tmp/dummy-vault source/rating/safe
cargo test --workspace
cargo deny check advisories bans licenses sources
```

## NixOS / Home Manager へのインストール

flake は x86_64 Linux 用の `packages.x86_64-linux.default` を提供します。
NixOS では `nixosModules.default` を import するか、
`environment.systemPackages` にパッケージを指定できます。

```nix
{
  inputs.kaede-gallery.url = "github:hanaretamae/Kaede-Gallery";

  outputs = { self, nixpkgs, kaede-gallery, ... }: {
    nixosConfigurations.my-host = nixpkgs.lib.nixosSystem {
      # ...
      modules = [
        kaede-gallery.nixosModules.default
        ./configuration.nix
      ];
    };
  };
}
```

Home Manager では `homeManagerModules.default` を import するか、
`home.packages = [ kaede-gallery.packages.${pkgs.system}.default ];` を設定します。
直接試す場合は `nix profile install github:hanaretamae/Kaede-Gallery` を使えます。

<details>
<summary>索引データベースの場所</summary>

既定は `$XDG_STATE_HOME/vault-gallery/index.sqlite`（なければ `$HOME/.local/state/vault-gallery/index.sqlite`）で、Vault の外です。
`--database <path>` で変更できますが、その場合も Vault の外である必要があります。
Unix では状態ディレクトリと索引ファイルの権限を現在のユーザーのみに制限します。
CLI のエラーメッセージに、ノートの内容・パス・タグ・URL は含まれません。

</details>

走査では隠しパスと Syncthing の管理ファイルを無視します。索引は使い捨てで、更新日時やサイズが変わると再パースし、
削除されたノートは次回の走査で消えます。メディアが見つからないノートは「存在フラグ」付きで残り、後の走査で再確認されます。

## Flutter ギャラリーの起動

```sh
nix develop
cd app
flutter pub get
GDK_BACKEND=wayland flutter run -d linux
```

<details>
<summary>Linux 環境の補足</summary>

- Nix シェルには Flutter、Linux デスクトップのビルド依存、Rust、Rustup、FFmpeg（動画サムネイル用）が含まれます。
- Rust ツールチェインは `rust-toolchain.toml` で固定され、初回ビルド時に Rustup が取得します。
- 動画再生には mpv / libass を使います。
- Linux ランナーは実行ファイル隣の共有ライブラリから Rust ブリッジを読み込むため、`LD_LIBRARY_PATH` の設定は不要です。
- ディレクトリ選択ダイアログは GTK ネイティブで、Flutter ではなくデスクトップの GTK テーマに従います。
- タイトルバーはコンポジタに任せ、独自の GTK ヘッダーバーは追加しません。
- `flake.nix` を変更したら `nix develop` を入り直してください。
- 画像サムネイルは純 Rust デコーダー（PNG・JPEG・GIF・WebP）、動画は Flutter ネイティブプラグインで生成します。AVIF はプレースホルダー表示です。

</details>

動画・外部ページ・Obsidian URI は、ユーザーがビューア上のボタンを押したときだけ開きます。

## Android 開発

Nix 開発シェルには Android SDK（API 35/36）、Build Tools 36、NDK 28.2、
CMake 3.22.1、JDK 17 が含まれます。Linux x86_64 で arm64 APK を作る場合、
Rust の Android ターゲットを一度追加してから、NDK のクロスコンパイラを指定します。

```sh
nix develop
rustup target add aarch64-linux-android
cd app
NDK_BIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin"
PATH="$(dirname "$(rustup which rustc)"):$PATH" \
  CC_aarch64_linux_android="$NDK_BIN/aarch64-linux-android35-clang" \
  CXX_aarch64_linux_android="$NDK_BIN/aarch64-linux-android35-clang++" \
  AR_aarch64_linux_android="$NDK_BIN/llvm-ar" \
  RANLIB_aarch64_linux_android="$NDK_BIN/llvm-ranlib" \
  CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$NDK_BIN/aarch64-linux-android35-clang" \
  flutter build apk --release --target-platform android-arm64
```

生成物は `app/build/app/outputs/flutter-apk/app-release.apk` です。署名鍵を設定しない
ローカルビルドは Android の debug keystore で署名されます。手元でのインストール確認には
使えますが、一般配布や別の署名鍵で署名済みのアプリの更新には使えません。

Documents 内の通常フォルダを SAF で選び、読み取り権限を保持します。
ノート本文は上限付きで Rust に渡して索引化し、画像・動画は選択フォルダ内の
URI から表示・再生します。Vault 本体はアプリ領域へコピーしません。
Syncthing 固有の連携は不要です。生成物はサイドロード可能な APK です。
Android 16 の実機では架空の Documents フォルダを使い、SAF での選択・再起動後の
権限保持・ノート索引・画像表示・短い MP4 の再生を確認しています。異なる端末や
Documents プロバイダ、メディア形式での追加確認は必要です。

### GitHub Release で配布

`v*` タグを push すると GitHub Actions がテスト後に arm64 の APK をビルドし、
Release に添付します。GitHub Actions の **Android APK** workflow を手動実行すると、
debug 鍵署名のテスト APK を artifact として取得できます（1 日で削除）。
手動実行には署名 Secrets は不要ですが、この APK は同じ debug 鍵で作られた
アプリの更新・検証用です。タグからのビルドでは Release asset のみを保存し、
Actions の保管量を抑えます。

このリポジトリは private です。GitHub Free では Actions の無料枠（月 2,000 分、
artifact / Packages 合計 500 MB）内で無料です。無料枠を超えても課金されないよう、
GitHub の [**Settings → Billing and licensing → Budgets and alerts**](https://github.com/settings/billing)
で GitHub Actions の予算を `$0` にし、「予算到達時に使用を停止」を有効にしてください
（[予算設定の公式ガイド](https://docs.github.com/en/billing/how-tos/set-up-budgets)）。
無料枠を
使い切ると Actions が停止し、その月の残りはビルドできません。標準 runner が
無制限無料なのは public repository の場合です。public 化するとソースと履歴も
公開されるため、このリポジトリは private のままです。

署名鍵は JDK の `keytool` で無料で作成できます。パスワードはプロンプトで入力し、
生成した鍵を安全な場所にバックアップしてください。鍵を失うと既存インストール
への更新ができなくなります。

```sh
keytool -genkeypair -v -keystore kaede-gallery-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias kaede-gallery
base64 < kaede-gallery-release.jks | tr -d '\n'
```

出力された Base64 と、作成時の値をリポジトリの **Settings → Secrets and variables →
Actions** に、それぞれ `ANDROID_KEYSTORE_BASE64`、`ANDROID_KEYSTORE_PASSWORD`、
`ANDROID_KEY_ALIAS`（`kaede-gallery`）、`ANDROID_KEY_PASSWORD` として登録します。
鍵ファイルやパスワードは Git に追加しないでください。GitHub Actions の標準機能と
Android SDK の範囲で設定でき、有料サービスや有料署名サービスは使いません。

## 設定の構成

1. **外観** — テーマ（システム／ライト／ダーク）、ピュアブラック、Material You
2. **ページングと一覧表示** — 1ページの件数、件数表示、タイルの通し番号
3. **保管庫** — Vault の選択、再走査
4. **ノート** — ノート構造と表示（ブロック順序・見出し・Frontmatter）、タグ設定（ギャラリー対象タグ・非表示タグ・タグ色）
5. **このアプリについて** — 情報とライセンス、設定のインポート・エクスポート・リセット、ヘルプ

ノート構造の設定画面には、設定を反映した架空のノート例と Markdown が表示されます（Vault には書き込みません）。
構造を変更すると再走査して反映します。タグ設定は表示のみを制御し、Vault と索引内容は変更しません。

## ノート構造とパーサー

パーサーは次を受け付けます。

- BOM あり／なしの UTF-8、LF／CRLF
- `tags` が空でない YAML Frontmatter
- Markdown 見出し、メディア埋め込み、リンク（Markdown・Wikilink 両対応）
- 「関連」「覚書」セクション（見出し名は設定で変更可能）

初期の除外カテゴリは `moc`、`add`、`pin`、`source/art` です。これらはカテゴリ表示にのみ影響し、
該当タグのノートも検索・一覧には残ります。

| 入力上限 | 値 |
| --- | --- |
| ノート | 2 MiB |
| Frontmatter | 256 KiB |
| タグ数 | 256 |
| YAML のネスト | 64 階層 |

> [!WARNING]
> YAML エイリアスはデシリアライズ前に拒否します（展開攻撃対策）。実 Vault でエイリアスが必要になった場合は、
> 有効化の前にイベントベースのパーサーと明示的な展開上限へ置き換えてください。

## プライバシーとセキュリティ

- Vault へのアクセスは常に読み取り専用。パスは開く前に検証し、索引・キャッシュは Vault 外にのみ作成します。
- 主要クレートは `forbid(unsafe_code)`。依存は `Cargo.lock` で固定し、ネットワーク系クレートは `deny.toml` で禁止しています。
- ログ・エラーにノート内容を含めません。
- SQLite は `rusqlite` の `bundled`（C ライブラリ同梱）を使います。純 Rust 優先方針に対する明示的な例外で、
  システムの SQLite 差異を避けるためのものです。ネットワーククライアントではありません。
- 索引はキャッシュなので、削除しても Vault から再構築できます。

## パフォーマンスとテストデータ

実クリップを使わず、規模を模した架空データを生成できます。生成物はコミットしません。

```sh
cargo run --release -p gallery-cli --bin generate-dummy-vault -- /tmp/vault-7806 --notes 7806
cargo run --release -p gallery-cli --bin generate-dummy-vault -- /tmp/vault-20000 --notes 20000
tools/benchmark.sh 7806
```

`tools/benchmark.sh` はリリースビルドと走査時間（初回・再走査）を計測します。
リポジトリ内の `testdata/dummy-vault` は架空のデータです。

## リポジトリ構成

```text
crates/
  gallery-parse/    ノートパーサー
  gallery-core/     索引・検索・クエリ・サムネイル
  gallery-bridge/   flutter_rust_bridge の公開 API
  gallery-cli/      CLI とダミー Vault 生成
app/lib/
  core_api/         リポジトリ層・プロバイダー・設定モデル
  features/gallery/ 設定・タグ絞り込み・グリッド・ビューアー（part 分割）
docs/design.md      設計の基準
```

貢献方法は [CONTRIBUTING.md](CONTRIBUTING.md) を参照してください。ライセンスは [LICENSE](LICENSE) です。
