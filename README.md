# Kaede Gallery

タグ付けされた Obsidian Vault の note 向けの、オフライン・読み取り専用のギャラリーです。
Rust 製のパーサー・索引・CLI と、Flutter(Linux)製のギャラリー UI を実装済みです。
アプリは Vault を選択し、ローカルで索引を作り、階層タグで絞り込み、プライベートな画像サムネイルを遅延生成します。
ギャラリーは Flutter 標準 Material 3 の共通テーマを使い、Material 3 Expressive の公式指針を参考に
色の階層、形状、タッチ領域、動きを全画面で統一しています。Flutter は Expressive の全コンポーネントを
提供していないため、ネイティブ Material 3 部品の範囲で実装しています。
丸みのあるカードと色の階層を活かしたスタイルを採用し、
タグの枠は折りたたんだ状態で表示します。タグ絞り込みには全カテゴリ対象の検索欄があり、
`#タグ` / `-#タグ` / ノート名・タグ名のあいまい検索を組み合わせられます。
ソースの各カテゴリは一つの「ソース」枠にまとめ、`source/test` より下のタグ階層は
その枠内で `test` 配下に表示します。絞り込み後も開いたカテゴリは閉じません。
カードを開くと黒背景のアプリ内ビューアーでメディアを最大表示し、最初は詳細を隠します。タップすると投稿者を画像上、
投稿文（投稿者を除く）を画像下に含む詳細を Material 3 の配色で表示します。複数メディアはスワイプ、
左右キー、横方向トラックパッド操作で切り替えられます。フルスクリーン切替は画像・動画共通で、
メディアは既定のアプリまたはファイルマネージャーでも開けます。
覚書・関連は箇条書きの階層を字下げで表し、関連ノートはアプリ内で開けます。タグはカテゴリ色で表示します。
動画は透明な Material 3 操作部品から再生速度・1本ループ・ミュートを切り替えられます。
Obsidian と外部 URL は、それぞれのボタンを押したときだけ開きます。
設定では絞り込み対象タグのプレフィックス、非表示タグ、タグ色を追加・削除でき、
外観を含む設定をJSONにエクスポートできます。タグの絞り込み設定は表示だけを制御し、
Vault内のノートや索引を変更しません。
JSON設定はインポートもでき、保存済み設定の全削除と既定値へのリセットも選べます。
ノート構造の設定では、ギャラリー対象タグのプレフィックス、覚書・関連・投稿文の終端として扱う見出し、
タグ・タイトル・投稿URL・日時・カバー画像に使うFrontmatterキーを編集できます。
Markdown/Wikilinkの関連ノート解決方法も最短・ノート相対・Vaultルート相対から選べます。
変更時は再走査して反映し、初期値は現在のノート形式を維持します。
架空のノート例は設定内でMarkdownとして閲覧できますが、Vaultには書き込みません。

## 主な機能

- ページング: 1ページの件数を設定でき、「総件数 件中 先頭 - 末尾 件」を表示します。
  任意の位置へ飛ぶと対象ページと直前ページだけを読み込み、スクロールに合わせて前後を追加読み込みします。
- 検索: ノート名・`#タグ`・`-#タグ`・`&#タグ` とタグ名のあいまい検索に対応します。
- 表示方法: ノート単位／全メディアの切替、タイルの通し番号・複数画像数・覚書数・関連数の表示。
- 設定: 外観、ページングと一覧表示、保管庫（Vault と再走査）、ノート（構造とタグ設定）、
  このアプリについて（情報・ライセンス、インポート・エクスポート・リセット、ヘルプ）。
- 技術構成: Rust（`gallery-parse` / `gallery-core` / `gallery-bridge` / `gallery-cli`）と Dart/Flutter のみ。
  Vault は読み取り専用で、索引・キャッシュ・設定は Vault 外のアプリ専用データに保存します。
  ネットワーク通信は行いません（外部リンクはボタン操作時のみ既定のアプリで開きます）。
- 開発用ダミー Vault は `cargo run -p gallery-cli --bin generate-dummy-vault` で生成します。
- UI コードは `app/lib/features/gallery/` 配下に、設定・タグ絞り込み・グリッド・ビューアーなどの
  Dart `part` ファイルとして分割しています。
現在の Flutter UI と動画再生は Linux を対象とし、Android 対応は後のフェーズです。

## Phase 1 クイックスタート

Nix で開発シェルに入るか、Rust stable・C コンパイラを手元にインストールしてください。

```sh
nix develop
cargo run --release -p gallery-cli --bin generate-dummy-vault -- /tmp/dummy-vault --notes 100
cargo run --release -p gallery-cli -- scan /tmp/dummy-vault
cargo run --release -p gallery-cli -- categories /tmp/dummy-vault
cargo run --release -p gallery-cli -- list /tmp/dummy-vault source/rating/safe
cargo test --workspace
cargo deny check advisories bans licenses sources
```

索引データベースは既定で `$XDG_STATE_HOME/vault-gallery/index.sqlite`
(なければ `$HOME/.local/state/vault-gallery/index.sqlite`)に置かれ、Vault の外にあります。
カスタムのデータベースパスを指定する場合も、Vault の外である必要があります。
Unix では状態ディレクトリと索引ファイルの権限を現在のユーザーのみに制限します。
CLI のエラーメッセージには、note の内容・パス・タグ・URL を含めません。

走査では、隠しパスと一般的な Syncthing の管理ファイルを無視します。
索引は使い捨てで、note の更新日時やサイズが変わると再パースし、削除された note は次回の走査で索引から消えます。
メディアが見つからない note は、「存在フラグ」付きで残り、後の走査で再確認されます。

## Flutter(Linux)ギャラリー

Nix シェルには、Flutter、Linux デスクトップビルドに必要な依存関係、Rust、Rustup が含まれています。
Rust ツールチェインは `rust-toolchain.toml` に固定されており、最初のネイティブアセットビルド時に
Rustup 経由でそのツールチェインがインストールされます。

```sh
nix develop
cd app
flutter pub get
GDK_BACKEND=wayland flutter run -d linux
```

Linux で動画サムネイルを生成するため、開発シェルには FFmpeg が含まれます。
Linux の動画再生には mpv/libass を使います。動画・外部ページ・Obsidian URI は
ユーザーがビューアー上で操作した場合にだけ開きます。

Linux ランナーは、実行ファイルと同梱された共有ライブラリから Rust ブリッジを読み込むため、
`LD_LIBRARY_PATH` にバンドルディレクトリを含める必要はありません。
Nix シェルは、Linux のディレクトリ選択ダイアログに必要な GTK のコンパイル済み GSettings スキーマも提供します。
`flake.nix` を変更した場合は `nix develop` を再実行してください。
ディレクトリ選択ダイアログは GTK のネイティブ UI で、Flutter の配色ではなくデスクトップの GTK/システムテーマに従います。
Linux ランナーはタイトルバーの描画をコンポジタに任せており、独自の GTK ヘッダーバーは追加しません。

アプリは索引とサムネイルを Flutter のアプリケーションサポートディレクトリに保存し、Vault には書き込みません。
データディレクトリは Unix では現在のユーザーのみに権限が制限されます。Vault へのアクセスは常に読み取り専用です。
アプリのデータはローカルに保存され、バックグラウンドでのネットワーク通信・テレメトリ・アナリティクスは一切行いません。
純 Rust サムネイルデコーダーは PNG・JPEG・GIF・WebP に対応します。
動画サムネイルは Flutter のネイティブプラグインで各動画からフレームを抽出します。
AVIF はプレースホルダー表示になります。

## パーサーの対象範囲と安全性

パーサーは、BOM 付き/なしの UTF-8、LF/CRLF、`tags` が空でない YAML フロントマター、
Markdown の見出し、Markdown のメディア埋め込み・リンク、「関連」/「覚書」セクションを受け付けます。
提供されているサニタイズ済みの Vault サンプルは Markdown 埋め込みのみを使用しており、
Obsidian の wikilink 埋め込みは必須ではありません。
リソース制限は現状、note の最大サイズを 2 MiB、フロントマターの最大サイズを 256 KiB、
タグ数の上限を 256、YAML のネストの深さを 64 階層としています。

YAML のエイリアスはデシリアライズ前に拒否されます。採用している `serde_yaml`/libyaml ベースのパーサーは
設定可能なエイリアス展開上限を公開していないため、エイリアストークンを拒否することが
展開攻撃を避けるための保守的な方法です。引用符で囲まれたアスタリスクやコメントは許可されます。
実際の Vault の note でエイリアスが必要になった場合は、有効化する前に、
イベントベースのパーサーと明示的なエイリアス上限に置き換えてください。

SQLite は `rusqlite` の `bundled` 機能で提供されます。これは FFI 経由で SQLite の C ライブラリを
同梱するもので、純 Rust 依存を優先する方針に対する明示的な例外です。
一貫したローカル索引を提供し、システムの SQLite のばらつきを避けるためのものであり、
ネットワーククライアントではありません。索引は Vault の外にのみ作成されます。

初期の除外対象プレフィックスは `moc`、`add`、`pin`、`source/art` です。
これらはカテゴリの表示にのみ影響し、`source/art` タグの付いた note は検索対象・一覧表示対象のままです。
カテゴリの選択肢には親タグを含めず、タグによる絞り込みは同一カテゴリ内では OR、カテゴリ間では AND です。

## パフォーマンスとテストデータ

実際のクリップを使わずに、規模を模したテストデータを生成できます。

```sh
cargo run --release -p gallery-cli --bin generate-dummy-vault -- /tmp/vault-7806 --notes 7806
cargo run --release -p gallery-cli --bin generate-dummy-vault -- /tmp/vault-20000 --notes 20000
```

`tools/benchmark.sh`（シェルのみ） は、指定した note 数に対するリリースビルドと走査時間を記録します。
タグの引数の前に `--database <path>` を指定すると、カスタムの索引の場所を選べます。
生成した大規模なテストデータはコミットしません。リポジトリに含まれる小さな
`testdata/dummy-vault` のテストデータは架空のものです。

## 依存関係のセキュリティ

主要なクレートには `forbid(unsafe_code)` を付けています。依存関係のバージョンは
`Cargo.lock` で固定され、ネットワーク関連のパッケージは `deny.toml` で禁止されています。
索引はキャッシュであり、削除して Vault から再構築できます。
