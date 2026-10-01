# vault-gallery

タグ付けされた Obsidian Vault の note 向けの、オフライン・読み取り専用のギャラリーです。
Rust 製のパーサー・索引・CLI と、Flutter(Linux)製のギャラリー UI を実装済みです。
アプリは Vault を選択し、ローカルで索引を作り、階層タグで絞り込み、プライベートな画像サムネイルを遅延生成します。
ギャラリーは Material 3 Expressive のスタイルを採用し、タグの枠は折りたたみ可能で、
ピル型の選択肢チップと各タグ枠の先頭に「カテゴリ全体」の選択肢を並べます。
ビューワー、詳細パネル、動画再生、Android 対応は後のフェーズで実装します。

## Phase 1 クイックスタート

Nix で開発シェルに入るか、Rust stable・C コンパイラ・Python 3 を手元にインストールしてください。

```sh
nix develop
python3 tools/generate_dummy_vault.py /tmp/dummy-vault --notes 100
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

Linux ランナーは、実行ファイルと同梱された共有ライブラリから Rust ブリッジを読み込むため、
`LD_LIBRARY_PATH` にバンドルディレクトリを含める必要はありません。
Nix シェルは、Linux のディレクトリ選択ダイアログに必要な GTK のコンパイル済み GSettings スキーマも提供します。
`flake.nix` を変更した場合は `nix develop` を再実行してください。
Linux ランナーはタイトルバーの描画をコンポジタに任せており、独自の GTK ヘッダーバーは追加しません。

アプリは索引とサムネイルを Flutter のアプリケーションサポートディレクトリに保存し、Vault には書き込みません。
データディレクトリは Unix では現在のユーザーのみに権限が制限されます。Vault へのアクセスは常に読み取り専用です。
アプリのデータはローカルに保存され、バックグラウンドでのネットワーク通信・テレメトリ・アナリティクスは一切行いません。
現状の純 Rust サムネイルデコーダーは PNG・JPEG・GIF・WebP に対応しています。
AVIF と動画サムネイルは、今後のメディア対応作業までプレースホルダー表示になります。

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
python3 tools/generate_dummy_vault.py /tmp/vault-7806 --notes 7806
python3 tools/generate_dummy_vault.py /tmp/vault-20000 --notes 20000
```

`tools/benchmark.sh` は、指定した note 数に対するリリースビルドと走査時間を記録します。
タグの引数の前に `--database <path>` を指定すると、カスタムの索引の場所を選べます。
生成した大規模なテストデータはコミットしません。リポジトリに含まれる小さな
`testdata/dummy-vault` のテストデータは架空のものです。

## 依存関係のセキュリティ

主要なクレートには `forbid(unsafe_code)` を付けています。依存関係のバージョンは
`Cargo.lock` で固定され、ネットワーク関連のパッケージは `deny.toml` で禁止されています。
索引はキャッシュであり、削除して Vault から再構築できます。
