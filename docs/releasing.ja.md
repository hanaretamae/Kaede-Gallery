# ビルド・リリース手順

アプリの唯一のバージョン源は `VERSION` です。正式リリースのタグは `VERSION` と完全一致させ、先頭に `v` を付けません。`2.0.0b4` のような beta タグは、バージョンだけをタイトルにした GitHub prerelease になります。

## 方法を選ぶ

| 方法 | 成果物 | 実行場所 |
| --- | --- | --- |
| 完全なリリース（2.0.0b4向け） | production署名済みuniversal／arm64-v8a／x86_64 Android APK、x86_64/AArch64 Linux bundle、チェックサム、GitHub prerelease | ローカルLinuxで `tools/release.sh` を実行し、その後Windows ZIPをActionsで添付 |
| 任意のリモートビルド | Android debug APK、および／またはLinux AArch64 bundleの一時artifact | 手動起動するActions。production releaseではない |
| 署名付きリリースを全てActionsで実行 | **現在は未対応** | Android production署名から完全なRelease作成までを行うworkflowはまだない |

## 現在の2.0.0b4の状態

`2.0.0b4` のソースcommitと対応するtagのpushは、production releaseの公開とは別です。tagを確認した後、ローカルのrelease scriptで署名済み成果物とGitHub prereleaseを作成してください。過去の `2.0.0b3` と `2.0.0b2` のtagを移動・作り直ししないでください。

タグ作成後に許されるのは、`CHANGELOG.md` 以外のMarkdownドキュメント変更だけです。リリーススクリプトは現在のコミットと `origin/main` が一致することも検査します。リリースノートとドキュメント以外のリリースコードはタグ時点のままにしてください。

## ローカルLinuxで完全なリリースを作成

最新の `main` を使い、`VERSION` が `2.0.0b4` であること、作業ツリーに未コミット変更がないことを確認します。

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
tools/release.sh 2.0.0b4
```

既定のKeePassXCエントリ名は `Kaede Gallery Android signing`、添付ファイル名は `release.jks` です。スクリプトは鍵を一時的に取り出し、universal／arm64-v8a／x86_64 APKの署名と検証、Rust/Kotlinテスト、x86_64/AArch64 Linux bundleの作成、チェックサム計算を行い、バージョンだけをタイトルにしたprereleaseを公開します。Rustup 1.98.1と、事前導入済みの `aarch64-linux-android`／`x86_64-linux-android` targets が必要です。Linux bundle は x86_64 Linux builder で作成します。AArch64 向け Rust FFI をクロスコンパイルし、target runtime を組み立てるため、QEMU/binfmt、AArch64 native builder、remote builder は不要です。`XDG_RUNTIME_DIR` は現在のユーザー所有でmode `700` のprivate directoryでなければなりません。

prereleaseの作成後、Windows ZIPを手動workflowで作成・添付します。

```sh
gh workflow run windows.yml \
  --repo hanaretamae/Kaede-Gallery \
  --ref main \
  --field tag=2.0.0b4
gh run list --repo hanaretamae/Kaede-Gallery --workflow windows.yml
```

このworkflowはtagとversionを検査し、Windows x64 packageを作り、ZIPと更新後のチェックサムを既存Releaseへアップロードします。Release自体は作成しません。

## 任意のリモートビルド

Android／Linux ARMのartifactだけを作る場合は、Nix開発環境に入り、CLI wrapperから起動します。refの既定値は `main` です。第2引数にはworkflowを含む別のbranchまたはtagを指定できます。

```sh
nix develop
tools/run-optional-builds.sh android-debug
tools/run-optional-builds.sh linux-aarch64
tools/run-optional-builds.sh both
```

1回の起動につき、いずれか1つを選んでください。GitHub Actions画面の **Optional Android and Linux AArch64 builds** からも起動できます。これは手動専用で、ローカルビルドを通常の方法とし、artifactは14日間保存します。`android-debug` はdebug署名APKで、production APKではありません。`linux-aarch64` は x86_64 runner 上で AArch64 bundle をクロスビルドします。このworkflowはx86_64のrelease bundle、GitHub Release、production署名済みAndroid APKを作成・公開しません。

別の `windows.yml` workflowはtagなしならWindows artifactを作れますが、tagを指定した場合も、同じtagのGitHub Releaseがすでに存在するときに限りZIPを添付します。

## Actionsだけでproduction releaseを行う方法

production署名を含む完全なActions-only releaseは**現在未対応**です。現在の署名手順は、リリース担当者のローカルKeePassXCデータベースからkeystoreとpasswordを読み取ります。任意のビルドworkflowは一時的なdebug/build artifactを作るだけで、Windows workflowは先にReleaseが存在することを要求します。

全てをリモートで行うには、保護されたrelease workflow、Android署名keystore/passwordをGitHub Environment secretまたは外部secret managerから渡すことについての明示的な決定、署名済みAPKと全bundleのCI検証が必要です。KeePassXCデータベースやkey fileをアップロードしたり、署名素材をリポジトリファイルに置いたりしないでください。このリモート署名構成は未承認・未設定です。`2.0.0b4` はローカルリリース手順を使用してください。
