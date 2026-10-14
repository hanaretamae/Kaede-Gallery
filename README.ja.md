<p align="center">
  <img alt="Kaede Gallery のアイコン" src="kotlin/shared-assets/branding/kaede-gallery-icon.png" width="128" height="128">
</p>

<h1 align="center">Kaede Gallery</h1>

<p align="center">
  <a href="README.md">English</a> | 日本語
</p>

<p align="center">
  タグ付けされた Obsidian Vault のノートを、<b>オフライン・読み取り専用</b>で眺めるギャラリー。<br>
  Rust（パーサー・索引・CLI）と Kotlin Multiplatform / Compose（Linux / Android / Windows Desktop UI）で構築しています。
</p>

<p align="center">
  <img alt="Platform" src="https://img.shields.io/badge/platform-Linux%20%7C%20Android%20%7C%20Windows-informational">
  <img alt="Rust" src="https://img.shields.io/badge/core-Rust-orange">
  <img alt="Kotlin Multiplatform" src="https://img.shields.io/badge/UI-Kotlin%20Multiplatform-7F52FF">
  <img alt="既定でオフライン" src="https://img.shields.io/badge/network-offline%20by%20default-success">
</p>

> [!IMPORTANT]
> Kaede Gallery は読み取り専用で、Vault には書き込みません。
> 索引・サムネイル・設定は Vault 外のアプリ専用データに保存します。
>
> 自動でネットワーク通信を行わず、テレメトリやアナリティクスも送信しません。
> 外部 URL は、開く操作をした場合にのみブラウザーへ渡します。

## 目次

- [特徴](#特徴)
- [アプリを使う](#アプリを使う)
- [プライバシーとセキュリティ](#プライバシーとセキュリティ)
- [インストールと起動](#インストールと起動)
- [開発者向け情報](#開発者向け情報)

## 特徴

- **閲覧**: ノート単位と全メディア表示を切り替えられます。タイルには通し番号や、
  画像・覚書・関連項目の件数を表示できます。
- **ページング**: 1ページの件数を設定し、任意の位置へ移動できます。
  スクロールに合わせて前後のページを読み込みます。
- **Desktop の表示と操作**: ウィンドウはシステムの表示倍率に追従し、
  ギャラリーの列数は利用可能な幅に応じて変わります。ホイール・トラックパッド・
  縦ドラッグでスクロールでき、設定で列数を固定することもできます。
- **検索**: ノート名とパスをあいまい検索できます。
  `#タグ`（OR）、`+#タグ`（AND）、`-#タグ`（除外）でタグも検索できます。
  `&#タグ` も AND として使えます。
- **絞り込み**: 階層タグを使って絞り込みます。
  同じカテゴリー内は OR、カテゴリー間は AND で判定します。
  作成日・公開日と並び順も選べます。
- **ビューアー**: メディアを大きく表示します。
  タップすると投稿者・投稿文・覚書・関連項目を確認できます。
  スワイプ・左右キー・トラックパッドでメディアを切り替えます。
- **関連リンク**: Markdown リンクと Wikilink から、Vault 内のノートを開けます。
- **動画**: mpv で再生し、速度・ループ・ミュートを操作できます。
  サムネイルは FFmpeg で作成します。
- **外観**: Material You の色を使い、システム・ライト・ダーク・ピュアブラックを選べます。
  フォントはシステムまたは Google Sans Flex から選べます。
- **言語**: 日本語と英語に対応しています。システム言語が日本語なら初期表示も日本語です。
- **ノート構造**: 投稿者・メディア・投稿文・関連・覚書の順序や、
  見出し名・Frontmatter のキーを設定できます。

> [!NOTE]
> Kotlin Multiplatform / Compose と Rust で開発しています。
> Android と Linux Desktop 向けにビルドできます。
> Windows x64／ARM64 の配布物もありますが、Windows 実機での動作は未確認です。
>
> [最新のリリースをダウンロード](https://github.com/hanaretamae/Kaede-Gallery/releases)
>
> ビルド・リリースの詳細は[リリース手順](docs/releasing.ja.md)を参照してください。

## アプリを使う

### 動作するノートの例

次のようなノートがギャラリーに表示されます（見出し名・ギャラリー対象タグは設定で変更できます。架空の例です）。

```markdown
---
created: 2026-01-02
tags:
  - source/art
  - source/type/illustration
  - source/count/1
cover: media/sample.png
---

# 夏の風景

![](media/sample.png)

# 文書

投稿文はここに書きます。

## 関連

- [関連ノート](other-note.md)
- [[another-note]]

## 覚書

- ビューアーの詳細に表示される覚書
```

- `tags` が空でない YAML Frontmatter が必要です。ギャラリー対象タグ（初期値 `source/art`）を含むノートが表示されます。
- 画像・動画は `![](…)` または `![[…]]` で埋め込みます。
- 「関連」「覚書」などの見出し名は、設定画面で日本語・英語どちらにも変更できます（`Related` / `Memo` / `Notes` / `Document` も初期値で認識します）。

### 設定の構成

1. **外観** — 設定の先頭にテーマ・言語・フォントを別々のリストで表示。ピュアブラックは背景だけを黒にし、他の色は Material You を維持します。ページング・一覧表示・メディアを表示できないときのアイコンは別ページ
2. **保管庫** — Vault の選択、再走査
3. **ノート** — ノート（ブロック順序・見出し・フロントマターキー）とタグ（ギャラリー対象タグ・表示対象タグ・非表示タグ・カテゴリー・タグ色）
4. **このアプリについて** — 情報とライセンス、設定のインポート・エクスポート・リセット、ヘルプ

Linux の設定インポート／エクスポートはシステムのファイル選択ポータルを使います。
Android では Storage Access Framework を使います。

フィルターのカテゴリー（人数・アートスタイル・性別・メタ・レーティング・ソース・タイプ・作品・その他）は、
パスの完全一致（`source/art`）や配下すべて（`source/count/*`）で編集できます。
3階層目以降のタグを親タグ別に分けるかもカテゴリーごとに設定できます（初期値はオフ）。

フロントマターのキーは「ノート」の先頭で編集できます。
タグは追加・編集・削除できるリストで管理します。

「ノート」の「ノートの例」から、現在の設定と言語を反映した架空の例を表示し、
Markdown をコピーできます。Vault 選択画面には初期設定の例を表示します。
どちらも Vault には書き込みません。

ノート構造を変更すると再走査して反映します。
タグ設定は表示だけに適用され、Vault や索引の内容は変わりません。

### ノート構造とパーサー

パーサーは次を受け付けます。

- BOM あり／なしの UTF-8、LF／CRLF
- `tags` が空でない YAML Frontmatter
- Markdown 見出し、メディア埋め込み、リンク（Markdown・Wikilink 両対応）
- 「関連」「覚書」セクション（見出し名は設定で変更可能）

初期の非表示タグは `moc`、`add`、`pin`、`source/art` です。これらはカテゴリ表示にのみ影響し、
該当タグのノートも検索・一覧には残ります。

| 入力上限      | 値      |
| ------------- | ------- |
| ノート        | 2 MiB   |
| Frontmatter   | 256 KiB |
| タグ数        | 256     |
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

## インストールと起動

### Rust CLI クイックスタート

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

### KMP Linux Desktop パッケージ

flake の既定 package と `.#kmpDesktop` は x86_64 / aarch64 Linux 向けの Kotlin/Compose Desktop アプリです。
ビルド・起動、または GitHub flake からのインストールには次を使います。

```sh
nix develop
nix build .#kmpDesktop
nix run .#kmpDesktop
nix profile install github:hanaretamae/Kaede-Gallery#kmpDesktop
```

x86_64 Linux から QEMU/binfmt を使わずに AArch64 package をクロスビルドできます。

```sh
nix build --system x86_64-linux --no-link .#packages.x86_64-linux.aarch64Cross
nix bundle --system x86_64-linux --out-link /tmp/kaede-gallery-aarch64 .#packages.x86_64-linux.aarch64Cross
```

この package は x86_64 上で Rust FFI を AArch64 向けにコンパイルし、
AArch64 用 Compose runtime、JRE、launcher、Linux library を組み立てます。
AArch64 host では従来の native `aarch64-linux` package も利用できます。
`packages.<system>.default`、`nixosModules.default`、
`homeManagerModules.default` は native KMP package を選びます。
手動 Actions workflow でも AArch64 bundle を cross-build できます。

Nix package と Linux release bundle には、バージョンを固定した
`mpv`・`ffmpeg` と実行時ライブラリを同梱します。
動画再生とサムネイル生成に、ホストの `PATH` 上の実行ファイルは不要です。

Google Sans Flex はアプリ内に同梱しており、外観設定から選べます。

配布物は[最新のリリース](https://github.com/hanaretamae/Kaede-Gallery/releases)からダウンロードできます。

<details>
<summary>索引データベースの場所</summary>

既定は `$XDG_STATE_HOME/vault-gallery/index.sqlite`（なければ `$HOME/.local/state/vault-gallery/index.sqlite`）で、Vault の外です。
`--database <path>` で変更できますが、その場合も Vault の外である必要があります。
Unix では状態ディレクトリと索引ファイルの権限を現在のユーザーのみに制限します。
CLI のエラーメッセージに、ノートの内容・パス・タグ・URL は含まれません。

</details>

走査では隠しパスと Syncthing の管理ファイルを無視します。索引は使い捨てで、更新日時やサイズが変わると再パースし、
削除されたノートは次回の走査で消えます。メディアが見つからないノートは「存在フラグ」付きで残り、後の走査で再確認されます。

### KMP アプリの起動とパッケージ作成

Linux Desktop の開発・配布物作成には `kotlin` の Gradle プロジェクトを使います。

```sh
nix develop
cd kotlin
LD_LIBRARY_PATH="$(pkg-config --variable=libdir gl):${LD_LIBRARY_PATH}" ./gradlew :desktopApp:run
./gradlew :desktopApp:createDistributable
```

Nix シェルには JDK 17、Android SDK（API 35–37.1）、Build Tools 37、
NDK 28.2、CMake 3.22.1 が含まれます。

Android は通常ローカルでビルドします。Rust Android targets を用意し、
NDK cross-compiler を設定してから `kotlin/` で
`:androidApp:assembleDebug` を実行してください。
APK は `kotlin/androidApp/build/outputs/apk/debug/` に作成されます。
debug 用 application ID は `com.hanaretamae.kaede.kmpdebug` です。
詳しい設定は [CONTRIBUTING.ja.md](CONTRIBUTING.ja.md) を参照してください。
これは production 用の署名済み release build ではありません。

release build は Android 7.0（API 24）以降に対応します。

KMP 版は、以前の Flutter 版とは別の Android アプリとしてインストールされます。
旧アプリの設定・索引キャッシュ・Vault へのアクセス権限は引き継がれません。
KMP 版で Vault を選び直してください。

旧アプリを引き続き使える場合は、先に設定をエクスポートし、
KMP 版の設定画面から JSON をインポートできます。
索引とキャッシュは Vault の外に再作成します。Vault をコピーしたり変更したりすることはありません。

Android はローカルビルドを基本とします。
必要に応じて Actions タブから
`.github/workflows/optional-arm-builds.yml` を手動起動してください。
`android-debug`、`linux-aarch64`、`both` を選べます。

Workflow が GitHub 上で利用可能になれば、認証済み GitHub CLI を使い、
`nix develop` からも起動できます。

```sh
tools/run-optional-builds.sh android-debug
# linux-aarch64 / both も選べます。第2引数は任意の ref です。
```

push / pull request では起動せず、ビルド artifact のみを保存します。production APK の署名や Release 公開はしません。
`.github/workflows/kotlin-windows.yml` は push / pull request ごとに Windows x64 のテストとパッケージを行います。
手動起動する `.github/workflows/windows.yml` はバージョン付き Windows ZIP を作り、tag を指定すれば既存 Release に添付します。
`tools/release.sh` は Linux から署名済み Android APK と Linux bundle を作成するリリース入口です。
Rustup 1.98.1 の Android targets が事前にインストール済みである必要があり、署名鍵は KeePassXC から取得します。
Windows 実機での動作は未検証です。
ローカルビルド・任意 Actions・リリースの制約は
[リリース手順](docs/releasing.ja.md)を参照してください。

## 開発者向け情報

### パフォーマンスとテストデータ

実クリップを使わず、規模を模した架空データを生成できます。生成物はコミットしません。

```sh
cargo run --release -p gallery-cli --bin generate-dummy-vault -- /tmp/vault-7806 --notes 7806
cargo run --release -p gallery-cli --bin generate-dummy-vault -- /tmp/vault-20000 --notes 20000
tools/benchmark.sh 7806
```

`tools/benchmark.sh` はリリースビルドと走査時間（初回・再走査）を計測します。
リポジトリ内の `testdata/dummy-vault` には架空のデータを置いています。

### リポジトリ構成

```text
crates/
  gallery-parse/    ノートパーサー
  gallery-core/     索引・検索・クエリ・サムネイル
  gallery-ffi/      Kotlin 向け UniFFI API
  gallery-cli/      CLI と架空 Vault 生成
kotlin/
  core/             共通モデル・repository・設定・Rust adapter
  ui/app/           共通 Compose UI
  androidApp/       Android SAF・メディア連携
  desktopApp/       Linux / Windows JVM アプリ
docs/design.md      設計の基準
```

このリポジトリは配布用です。外部からの Issue・Pull Request は受け付けていません。
開発者向け手順は [CONTRIBUTING.ja.md](CONTRIBUTING.ja.md) を参照してください。

ライセンスは [MIT License](LICENSE) です。
同梱コンポーネントのライセンスは
[第三者ライセンス一覧](THIRD_PARTY_NOTICES.ja.md)と配布物内の notices を確認してください。
リリースと配布物は[リリース一覧](https://github.com/hanaretamae/Kaede-Gallery/releases)で確認できます。
