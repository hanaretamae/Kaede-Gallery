# ビルド・リリース手順

Kaede Gallery のビルド・公開に関する開発者向け手順です。
リリース固有の状況や配布物一覧は GitHub Release metadata または
versioned release notes に記録し、このガイドでは再利用する手順を説明します。

アーキテクチャとプロジェクトの制約は
[`docs/design.md`](design.md) を基準とします。
リリース・テストにユーザーの Vault を使わないでください。

## リリース経路

| 経路 | 成果物 |
| --- | --- |
| `tools/release.sh` によるローカル production release | 署名済み universal／arm64-v8a／x86_64 Android APK、自己完結した x86_64／AArch64 Linux `.tar.zst` archive と AppImage、ライセンス資料、チェックサム、GitHub Release |
| tag を指定した Windows 手動 workflow | Windows x64／ARM64 ZIP を既存 GitHub Release に添付し、チェックサムを更新 |
| tag なしの Windows 手動 workflow | GitHub Release へ添付しない Windows ZIP build artifact |
| 任意の Android/Linux workflow | Android debug APK、および／または AArch64 Linux bundle の一時 Actions artifact |

release tag は `VERSION` と完全一致させ、先頭に `v` を付けません。
beta (`bN`) または release candidate (`rcN`) suffix がある tag は prerelease になります。
`tools/release.sh` は `CHANGELOG.md` の一致する見出しから release note を読み込みます。

## リリースの準備

`VERSION` の変更、tag 作成、artifact の公開にはリリース担当者の承認が必要です。
tag を作成する前に次を確認してください。

- 実装差分と実際に確認した結果に基づき、`CHANGELOG.md` を更新・レビューする。
- 変更に必要な Rust / Kotlin check を実行する。リリーススクリプトも必要な test suite と安全性確認を実行する。
- レビュー済み変更を `main` に commit・push し、作業ツリーが clean であることを確認する。
  スクリプトは commit が `origin/main` と一致することを要求する。
- `VERSION` と release note の見出しが一致することを確認する。
  公開済み tag を再利用しない。

リリース担当者が version と changelog を承認した後、clean checkout で次を実行します。

```sh
git switch main
git pull --ff-only origin main
git fetch origin --tags
cat VERSION
git status --short
```

## Android / Linux artifact の公開

固定 Nix 開発環境、認証済み GitHub CLI、KeePassXC CLI、Android SDK/NDK、
Rustup 1.98.1 を備えた Linux host を使います。
`aarch64-linux-android` と `x86_64-linux-android` Rust target は、
その toolchain に事前インストールされている必要があります。
リリーススクリプトは Rust workspace test、Kotlin/JVM test、Android release の
build・検証を行い、Linux の両 architecture を作成します。
AArch64 Linux package は x86_64 Linux host から cross-build します。

開発 shell に入り、ローカルの KeePassXC database と key file を指定します。
ファイルの内容や署名情報をリポジトリ・コマンド引数・chat に含めないでください。

```sh
nix develop
export KEEPASSXC_DATABASE="/absolute/path/to/signing-database.kdbx"
export KEEPASSXC_KEY_FILE="/absolute/path/to/signing-key-file"
release_tag="$(cat VERSION)"
```

既定の KeePassXC entry は `Kaede Gallery Android signing`、
attachment は `release.jks` です。`XDG_RUNTIME_DIR` は現在のユーザーが所有する
mode `700` の private directory でなければなりません。
一時的な署名素材はこの場所に保存され、スクリプト終了時に削除されます。

レビュー済みリリース commit がすでに `origin/main` にあることを確認してから、
annotated tag を作成・push します。スクリプトは local/remote tag、source commit、
認証情報、release note、build 前提条件を検証してから公開します。

```sh
git tag -a "$release_tag" -m "$release_tag"
git push origin main "$release_tag"
tools/release.sh "$release_tag"
```

スクリプトが GitHub Release と Android/Linux artifact を作成した後、
Windows workflow を起動して両 architecture の ZIP を作成・添付します。

```sh
gh workflow run windows.yml \
  --repo hanaretamae/Kaede-Gallery \
  --ref main \
  --field tag="$release_tag"
gh run list --repo hanaretamae/Kaede-Gallery --workflow windows.yml
```

Windows workflow は先に Release が存在することを要求します。
tag と `VERSION` の一致を確認し、プロジェクトおよびサードパーティ通知とともに
`Kaede-Gallery.exe` を package 化して、release のチェックサムを更新します。

## 任意の artifact build

任意の Android/Linux workflow は手動起動のみです。push / pull request では起動せず、
production APK の署名や Release の公開も行いません。
Android debug APK、AArch64 Linux bundle、または両方を build できます。
artifact の保存期間は14日です。

`nix develop` 内から次を実行します。

```sh
tools/run-optional-builds.sh android-debug
tools/run-optional-builds.sh linux-aarch64
tools/run-optional-builds.sh both
```

第2引数で workflow ref を指定できます。認証済み GitHub CLI が必要です。

Windows 手動 workflow は tag を指定せずに起動すると、ZIP artifact の build のみ行います。
artifact の保存期間は7日です。tag を指定した場合は既存 Release に ZIP を添付します。

## 公開の確認

tag、workflow の定義、build 成功だけで公開済みとは判断しないでください。
公開中の GitHub Release に実際の artifact が存在することと、チェックサムを確認します。

```sh
gh release view "$release_tag" --repo hanaretamae/Kaede-Gallery
verify_dir="$(mktemp -d)"
gh release download "$release_tag" \
  --repo hanaretamae/Kaede-Gallery \
  --dir "$verify_dir"
(cd "$verify_dir" && sha256sum -c SHA256SUMS)
```

ダウンロードしたファイル名と内容を release note・対象 platform と照合してください。
確認後、一時ディレクトリを削除します。

## 署名の境界

Android production 署名は `tools/release.sh` がローカルで行い、
リリース担当者の KeePassXC database から認証情報を取得します。
Actions workflow だけでは production 署名から公開までを完結できません。
KeePassXC database や key file を upload したり、署名素材を repository に保存したりしないでください。
