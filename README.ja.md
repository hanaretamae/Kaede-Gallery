<p align="center">
  <img alt="Kaede Gallery のアイコン" src="kotlin/shared-assets/branding/kaede-gallery-icon.png" width="128" height="128">
</p>

<h1 align="center">Kaede Gallery</h1>

<p align="center">
  <a href="README.md">English</a> | 日本語
</p>

<p align="center">
  タグ付きの Obsidian Vault ノートをオフライン・読み取り専用で閲覧するギャラリーです。<br>
  Kotlin Multiplatform / Compose と Rust のパーサー・索引・FFI で構成しています。
</p>

> [!IMPORTANT]
> Kaede Gallery は Vault に書き込みません。索引・サムネイル・設定は
> Vault 外のアプリ専用データに保存します。
>
> 自動でネットワーク通信を行わず、テレメトリやアナリティクスも送信しません。
> 外部 URL は、開く操作をした場合にのみ開きます。

## 特徴

- ノート単位または全メディアを閲覧し、ページングや一覧レイアウトを設定できます。
- ノート名・パスをあいまい検索できます。タグ検索には
  `#タグ`（OR）、`+#タグ`（AND）、`-#タグ`（除外）を使えます。
- 階層タグで絞り込み、作成日または公開日で並べ替えできます。
- 画像・動画とともに、ノートの詳細・覚書・関連リンクを表示します。
- Markdown リンクと Obsidian Wikilink から、選択中の Vault 内のノートを開けます。
- 言語・外観・ノート見出し・フロントマターキー・タグルールを設定できます。

共通 UI は日本語と英語に対応しています。動画再生とサムネイル作成には
アプリのプラットフォーム連携を使います。

## アプリを使う

1. Vault フォルダーを選択します。アプリは読み取り専用でアクセスします。
2. 閲覧するノートにギャラリー対象タグを付けます。初期値は `source/art` です。
3. ギャラリーを検索・絞り込み、タイルを開いてメディアと詳細を表示します。

架空のノートは、たとえば次のように記述できます。

```markdown
---
created: 2026-01-02
tags:
  - source/art
  - source/type/illustration
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

空でない `tags` リストを含む YAML フロントマターが必要です。
画像・動画は `![](…)` または `![[…]]` で埋め込めます。
`Related`・`Memo` などの見出し名は設定でき、日本語の見出しにも対応します。

パーサーは BOM あり／なしの UTF-8 と LF／CRLF を受け付けます。
入力には上限があり、ノートは最大 2 MiB、フロントマターは最大 256 KiB、
タグは最大 256 個、YAML のネストは最大 64 階層です。
展開攻撃を防ぐため、YAML エイリアスはデシリアライズ前に拒否します。

## プライバシーと安全性

- Vault は常に読み取り専用です。ファイルを開く前にパスを検証します。
- 索引とキャッシュは再作成可能で、Vault 外のプライベートなアプリデータに保存します。
- Core／parser はオフラインかつ入力上限付きで動作し、ログやエラーにノート内容を含めません。
- Rust core は unsafe code を禁止し、ネットワーク依存を許可しません。
- SQLite はシステムごとの差異を避けるため、`rusqlite` 経由でライブラリを同梱します。

## ダウンロードとソースからのビルド

[GitHub Releases](https://github.com/hanaretamae/Kaede-Gallery/releases)
で公開中の配布物を確認できます。ビルドと検証の手順は
[CONTRIBUTING.ja.md](CONTRIBUTING.ja.md)、リリース手順は
[リリースガイド](docs/releasing.ja.md)を参照してください。

固定された開発環境で架空データを使って Rust CLI を試すには、次を実行します。

```sh
nix develop
cargo run --release -p gallery-cli --bin generate-dummy-vault -- /tmp/kaede-gallery-demo --notes 100
cargo run --release -p gallery-cli -- scan /tmp/kaede-gallery-demo
```

リポジトリの `testdata/dummy-vault` にも架空のノートとメディアがあります。
個人の Vault をテストデータとして使わないでください。

## プロジェクト情報

- [設計と実装の基準](docs/design.md)
- [開発環境とチェック](CONTRIBUTING.ja.md)
- [サードパーティ通知](THIRD_PARTY_NOTICES.ja.md)
- [MIT License](LICENSE)
