# ビルド・リリース手順

アプリの唯一のバージョン源は `VERSION` です。正式リリースのタグは `VERSION` と完全一致させ、先頭に `v` を付けません。`2.0.0b5` のような beta と `2.0.0rc1` のような release candidate は、バージョンだけをタイトルにした GitHub prerelease になります。

## 方法を選ぶ

| 方法 | 成果物 | 実行場所 |
| --- | --- | --- |
| 完全なリリース（安定版／beta／release candidate） | production署名済みuniversal／arm64-v8a／x86_64 Android APK、x86_64/AArch64 Linux bundle（`.tar.zst` と `.AppImage`）、チェックサム、バージョン名のGitHub Release | ローカルLinuxで `tools/release.sh` を実行し、その後Windows ZIPをActionsで添付 |
| 任意のリモートビルド | Android debug APK、および／またはLinux AArch64 bundleの一時artifact | 手動起動するActions。production releaseではない |
| 署名付きリリースを全てActionsで実行 | **現在は未対応** | Android production署名から完全なRelease作成までを行うworkflowはまだない |

## 現在のリリース状況

2026-10-12にGitHub上の公開状態を確認しました。安定版 `2.0.1` は公開済みです（[Releaseページ](https://github.com/hanaretamae/Kaede-Gallery/releases/tag/2.0.1)）。universal／arm64-v8a／x86_64 Android APK、Linux x86_64／AArch64 の `.tar.zst` と `.AppImage`、`SHA256SUMS`、ライセンス資料が登録されています。確認時点では Windows x64／ARM64 の Release workflow [38239334210](https://github.com/hanaretamae/Kaede-Gallery/actions/runs/38239334210) が実行中だったため、Windows archive は Release ページへの添付を確認できていません。Windows 配布物の有無を記載するときは Release ページを再確認してください。安定版 `2.0.0` も Android、Linux x86_64／AArch64、Windows x64／ARM64 の配布物とともに公開済みです（[Releaseページ](https://github.com/hanaretamae/Kaede-Gallery/releases/tag/2.0.0)）。

以前の `2.0.0b4` prerelease にはWindows ZIPがありません。Windows workflow run [38136147012](https://github.com/hanaretamae/Kaede-Gallery/actions/runs/38136147012) は `LinuxPortalDirectoryPickerTest.resolvesPortalImportAndExportFilesWithoutAcceptingNonLocalUris` で失敗し、archive作成とuploadはskipされました。その後、Release owner は安定版の配布物と成功したWindows workflow run [38199667594](https://github.com/hanaretamae/Kaede-Gallery/actions/runs/38199667594) を根拠にPhase 14を完了扱いとしました。b4固有の欠落は明示的に免除された歴史的項目であり、ZIPをアップロード済みとは記録しません。過去のtagを移動・作り直ししないでください。

Android version codeではbetaをb1〜b898、release candidateをrc1〜rc100、安定版を999として予約します。タグ作成後に許されるのは、`CHANGELOG.md` 以外のMarkdownドキュメント変更だけです。リリーススクリプトは現在のコミットと `origin/main` が一致することも検査します。リリースノートとドキュメント以外のリリースコードはタグ時点のままにしてください。

## 公開済み 2.0.1 Release

安定版 `2.0.1` は2026-10-12にprereleaseではないGitHub Releaseとして公開されました。Releaseページには3種類のAndroid APK、Linux x86_64／AArch64 の `.tar.zst` と AppImage、チェックサム、ライセンス資料があります。2.0.1以降のLinux bundleには固定した `mpv`／`ffmpeg` と実行時依存を同梱するため、2.0.0のLinux配布物と異なりホストの `PATH` にこれらを用意する必要はありません。Windows x64／ARM64 archive は別workflowで作成されます。公開済みと記載する前にReleaseページ上の添付を確認してください。Windows実機や未検証のhardware/providerの動作は未確認のままです。

## ローカルLinuxで完全なリリースを作成

以下の手順は今後の未公開リリース向けです。`2.0.1` は公開済みのため、このtagでコマンドを再実行しないでください。将来のリリースでは、レビュー済み `main` commitを使い、`VERSION` がRelease tagと一致すること、作業ツリーに未コミット変更がないことを確認します。

```sh
git switch main
git pull --ff-only origin main
git fetch origin --tags
cat VERSION
git status --short
```

KeePassXCデータベースとキーファイルのパスをローカル環境変数に設定します。ファイルの内容や秘密鍵そのものをリポジトリ、コマンド引数、チャットに入れないでください。

```sh
export KEEPASSXC_DATABASE="/absolute/path/to/signing-database.kdbx"
export KEEPASSXC_KEY_FILE="/absolute/path/to/signing-key-file"
release_tag="$(cat VERSION)"
tools/release.sh "$release_tag"
```

既定のKeePassXCエントリ名は `Kaede Gallery Android signing`、添付ファイル名は `release.jks` です。スクリプトは鍵を一時的に取り出し、universal／arm64-v8a／x86_64 APKの署名と検証、Rust/Kotlinテスト、x86_64/AArch64 Linux bundleの作成、チェックサム計算を行い、tagに応じた安定版またはprereleaseを公開します。2.0.1以降のLinux `.tar.zst` と `.AppImage` は自己完結したNix closureを含み、固定した `mpv`・`ffmpeg` と実行時ライブラリ、ライセンス資料を同梱します。bundle作成前に両アーキテクチャのpackage closureへ両ツールが含まれることをスクリプトで検査します。Rustup 1.98.1と、事前導入済みの `aarch64-linux-android`／`x86_64-linux-android` targets が必要です。Linux bundle は x86_64 Linux builder で作成します。AArch64 向け Rust FFI をクロスコンパイルし、target runtime を組み立てるため、QEMU/binfmt、AArch64 native builder、remote builder は不要です。`XDG_RUNTIME_DIR` は現在のユーザー所有でmode `700` のprivate directoryでなければならず、署名素材はこの中に一時保存します。大容量のrelease一時artifactは、容量の小さいruntime tmpfsを使い切らないよう、`TMPDIR`（既定値 `/tmp`）配下のprivate directoryに保存します。

将来のGitHub Release作成後、Windows ZIPを手動workflowで作成・添付します。

```sh
gh workflow run windows.yml \
  --repo hanaretamae/Kaede-Gallery \
  --ref main \
  --field tag="$(cat VERSION)"
gh run list --repo hanaretamae/Kaede-Gallery --workflow windows.yml
```

このworkflowはtagとversionを検査し、Windows x64 packageを作り、ZIPと更新後のチェックサムを既存Releaseへアップロードします。Release自体は作成しません。両方のWindows workflowは `Kaede-Gallery.exe` と同じ階層に `LICENSE` とその日本語参考訳、英語・日本語のREADMEとサードパーティライセンス通知を配置します。配布前にartifact内の各ファイルを確認してください。

## 任意のリモートビルド

Android／Linux ARMのartifactだけを作る場合は、Nix開発環境に入り、CLI wrapperから起動します。refの既定値は `main` です。第2引数にはworkflowを含む別のbranchまたはtagを指定できます。

```sh
nix develop
tools/run-optional-builds.sh android-debug
tools/run-optional-builds.sh linux-aarch64
tools/run-optional-builds.sh both
```

1回の起動につき、いずれか1つを選んでください。GitHub Actions画面の **Optional Android and Linux AArch64 builds** からも起動できます。これは手動専用で、ローカルビルドを通常の方法とし、artifactは14日間保存します。`android-debug` はdebug署名APKで、production APKではありません。`linux-aarch64` は x86_64 runner 上で AArch64 bundle をクロスビルドします。このworkflowはx86_64のrelease bundle、GitHub Release、production署名済みAndroid APKを作成・公開しません。

自動実行の `kotlin-windows.yml` artifact と手動実行の `windows.yml` ZIP には、チェックサムを検証したWindows版mpv／FFmpegを `tools/` に、ライセンステキストを `licenses/` に同梱します。手動workflowはtagなしならWindows artifactを作れますが、tagを指定した場合も、同じtagのGitHub Releaseがすでに存在するときに限りZIPを添付します。

## Actionsだけでproduction releaseを行う方法

production署名を含む完全なActions-only releaseは**現在未対応**です。現在の署名手順は、リリース担当者のローカルKeePassXCデータベースからkeystoreとpasswordを読み取ります。任意のビルドworkflowは一時的なdebug/build artifactを作るだけで、Windows workflowは先にReleaseが存在することを要求します。

全てをリモートで行うには、保護されたrelease workflow、Android署名keystore/passwordをGitHub Environment secretまたは外部secret managerから渡すことについての明示的な決定、署名済みAPKと全bundleのCI検証が必要です。KeePassXCデータベースやkey fileをアップロードしたり、署名素材をリポジトリファイルに置いたりしないでください。このリモート署名構成は未承認・未設定です。`2.0.1` はすでに公開済みです。ローカル手順は今後の承認済みversionで使用してください。
