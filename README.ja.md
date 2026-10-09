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
  <img alt="Offline" src="https://img.shields.io/badge/network-none-success">
</p>

> [!IMPORTANT]
> Vault には**一切書き込みません**。索引・サムネイル・設定はすべて Vault の外のアプリ専用データに保存します。
> ネットワーク通信・テレメトリ・アナリティクスもありません。

## 目次

- [特徴](#特徴)
- [アプリを使う](#アプリを使う)
- [プライバシーとセキュリティ](#プライバシーとセキュリティ)
- [インストールと起動](#インストールと起動)
- [開発者向け情報](#開発者向け情報)

## 特徴

| 分野       | 内容                                                                                                                                    |
| ---------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| 閲覧       | ノート単位／全メディアの表示切替。タイルに通し番号・複数画像数・覚書数・関連数を表示可能                                                |
| ページング | 1ページの件数を設定可能。「総件数 件中 先頭 - 末尾 件」を表示し、任意の位置へジャンプ。スクロールに合わせて前後を追加読み込み           |
| 検索       | ノート名、`#タグ`、`-#タグ`、`&#タグ`、タグ名のあいまい検索                                                                             |
| 絞り込み   | 階層タグを +／−／AND／無選択で絞り込み。同カテゴリ内は OR、カテゴリ間は AND。作成日・公開日の昇順／降順（初期値は作成日降順）も選択可能 |
| ビューアー | 黒背景でメディアを最大表示。タップで投稿者・投稿文・覚書・関連などの詳細を表示。複数メディアはスワイプ／左右キー／トラックパッドで切替  |
| 関連リンク | Markdown リンクと Wikilink（`![[…]]`・`![](…)` を含む）の指す先がノートなら、アプリ内の詳細ビューで開く                                 |
| 動画       | mpv による再生。再生速度・1本ループ・ミュート。サムネイルは FFmpeg ベース                                                               |
| 外観       | Material 3 Expressive を意識した配色・形状・動き、Material You（動的カラー）、システム／ライト／ダーク／ピュアブラック                  |
| ノート構造 | ブロック順序（投稿者・メディア・投稿文・投稿文の終端・関連・覚書）、見出し名・レベル・箇条書きの有無、Frontmatter キーを設定可能        |

> [!NOTE]
> 現在の UI は Kotlin Multiplatform / Compose です。Android と Linux Desktop のビルド手順があります。
> Windows x64 のテスト・パッケージ作成と手動 Release workflow も用意していますが、実機での実行は未確認です。
> 署名済み Android release の公開はローカルのリリーススクリプトを使い、リリース用認証情報での検証が必要です。

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

1. **外観** — 言語（システム／日本語／English。日本語以外のシステムでは英語が既定）、テーマ（システム／ライト／ダーク）、ピュアブラック、Material You
2. **ページングと一覧表示** — 1ページの件数、件数表示、タイルの通し番号
3. **保管庫** — Vault の選択、再走査
4. **ノート** — ノート構造と表示（ブロック順序・見出し・Frontmatter）、タグ設定（ギャラリー対象タグ・フィルターのカテゴリー・非表示タグ・タグ色）。各設定には「初期設定に戻す」があります
5. **このアプリについて** — 情報とライセンス、設定のインポート・エクスポート・リセット、ヘルプ

フィルターのカテゴリー（人数・アートスタイル・性別・メタ・レーティング・ソース・タイプ・作品・その他）は、パスの完全一致（`source/art`）や配下すべて（`source/count/*`）で編集できます。カテゴリーごとに、3階層目以降のタグを親タグ別に分けるかを選べます（初期はオフ）。

ノート構造の設定画面には、設定を反映した架空のノート例と Markdown が表示されます（Vault には書き込みません）。
構造を変更すると再走査して反映します。タグ設定は表示のみを制御し、Vault と索引内容は変更しません。

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

x86_64 から aarch64 をクロスビルドするには QEMU/binfmt と Nix の `extra-platforms` が必要です。ネイティブまたは remote の AArch64 builder なら同じコマンドをエミュレーションなしで実行できます。

```sh
nix config show | grep '^extra-platforms'
nix build --system aarch64-linux --no-link .#packages.aarch64-linux.kmpDesktop
```

この AArch64 package build は 2026-10-09 にオーナーが成功を確認しました（builder の方式は未記録）。パッケージには Compose Desktop の配布物、Linux ランチャー、ライセンス通知が含まれます。`packages.<system>.default`、`nixosModules.default`、`homeManagerModules.default` はすべて KMP を選びます。自己完結型の AArch64 Linux bundle は、後述する任意の手動 Actions workflow からも作成できます。

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

Nix シェルには JDK 17、Android SDK（API 35–37）、Build Tools 37、NDK 28.2、CMake 3.22.1 が含まれます。Android は通常ローカルでビルドします。Rust Android targets を用意し、NDK cross-compiler を設定してから `kotlin/` で `:androidApp:assembleDebug` を実行してください。APK は `kotlin/androidApp/build/outputs/apk/debug/` に作成され、debug 用 application ID は `com.hanaretamae.kaede.kmpdebug` です。詳しい設定コマンドは [CONTRIBUTING.ja.md](CONTRIBUTING.ja.md) を参照してください。production 用の署名済み release build とは異なります。

KMP Android の production application ID は `com.hanaretamae.kaede`、以前の Flutter Android ID は
`com.hanaretamae.vault_gallery` です。Android では別アプリとして扱われ、KMP を入れても Flutter 版は
更新されず、Flutter の非公開設定・索引キャッシュ・SAF の永続権限も引き継がれません。KMP で Vault を
選び直し、必要なら旧アプリで設定をエクスポートして、設定画面から互換性のある JSON を手動で
インポートしてください。索引・キャッシュは Vault 外で再作成され、Vault 自体はコピー・変更しません。

Android は引き続きローカルビルドを基本とします。必要時は Actions タブから
`.github/workflows/optional-arm-builds.yml` を手動起動し、`android-debug`、`linux-aarch64`、`both` を選べます。
Workflow が GitHub 上で利用可能になった後は、認証済み GitHub CLI を使って `nix develop` からも起動できます。

```sh
tools/run-optional-builds.sh android-debug
# linux-aarch64 / both も選べます。第2引数は任意の ref です。
```

push / pull request では起動せず、ビルド artifact のみを保存します。production APK の署名や Release 公開はしません。
`.github/workflows/kotlin-windows.yml` は push / pull request ごとに Windows x64 のテストとパッケージを行います。
手動起動する `.github/workflows/windows.yml` はバージョン付き Windows ZIP を作り、tag を指定すれば既存 Release に添付します。
`tools/release.sh` は Linux から署名済み Android APK と Linux bundle を作成するリリース入口です。
Rustup 1.98.1 の Android targets が事前にインストール済みである必要があり、署名鍵は KeePassXC から取得します。
Windows 実機での動作と実際の鍵を使った Android release はここでは未検証です。

## 開発者向け情報

### パフォーマンスとテストデータ

実クリップを使わず、規模を模した架空データを生成できます。生成物はコミットしません。

```sh
cargo run --release -p gallery-cli --bin generate-dummy-vault -- /tmp/vault-7806 --notes 7806
cargo run --release -p gallery-cli --bin generate-dummy-vault -- /tmp/vault-20000 --notes 20000
tools/benchmark.sh 7806
```

`tools/benchmark.sh` はリリースビルドと走査時間（初回・再走査）を計測します。
リポジトリ内の `testdata/dummy-vault` は架空のデータです。

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

このリポジトリは配布のみを目的とし、外部からの Issue・Pull Request は受け付けていません。開発者向け手順は [CONTRIBUTING.ja.md](CONTRIBUTING.ja.md) を参照してください。ライセンスは [LICENSE](LICENSE)（MIT）です。同梱コンポーネントのライセンスは [THIRD_PARTY_NOTICES.ja.md](THIRD_PARTY_NOTICES.ja.md) と各配布物に含まれる notices を確認してください。KMP の Release artifact はまだ公開・検証されていません。
