{
  description = "Kaede Gallery packages and development environment";

  inputs.nixpkgs.url = "https://channels.nixos.org/nixos-unstable/nixexprs.tar.zst";

  outputs =
    { self, nixpkgs }:
    let
      systems = [
        "x86_64-linux"
        "aarch64-linux"
      ];
      forAllSystems = nixpkgs.lib.genAttrs systems;
      version = nixpkgs.lib.removeSuffix "\n" (builtins.readFile ./VERSION);
      mkPkgs =
        system:
        import nixpkgs {
          inherit system;
          config = {
            allowUnfree = true;
            android_sdk.accept_license = true;
          };
        };
    in
    {
      packages = forAllSystems (
        system:
        let
          pkgs = mkPkgs system;
          cargoVendor = pkgs.rustPlatform.fetchCargoVendor {
            src = self;
            cargoLock.lockFile = ./Cargo.lock;
            hash = "sha256-DWOl56PyBaCo9CtGuEy3GrSCy3O8IRVHpZHHeQkB7Sc=";
          };
          bindgenSource = pkgs.fetchFromGitHub {
            owner = "UbiqueInnovation";
            repo = "uniffi-kotlin-multiplatform-bindings";
            rev = "v1.3.0";
            hash = "sha256-TB5DpME69P5LwC17uz51M/mrTvmtOZ17U5TJyN3pBBo=";
          };
          bindgenVendor = pkgs.rustPlatform.fetchCargoVendor {
            src = bindgenSource;
            cargoLock.lockFile = "${bindgenSource}/Cargo.lock";
            hash = "sha256-gUJnb7jCvq5v3m/rKTbG0NNV+51ub7S32vYIftOhips=";
          };
          rustupShim = pkgs.writeShellScriptBin "rustup" ''
            if [ "$#" -eq 2 ] && [ "$1" = "show" ] && [ "$2" = "active-toolchain" ]; then
              printf '%s\n' '1.98.1 (Nix package)'
            elif [ "$#" -ge 3 ] && [ "$1" = "run" ] && [ "$2" = "1.98.1" ]; then
              shift 2
              exec "$@"
            else
              echo "Unsupported rustup command in the Nix build." >&2
              exit 1
            fi
          '';

          gradle = pkgs.gradle_9.override { java = pkgs.jdk17; };
          kmpDesktopItem = pkgs.makeDesktopItem {
            name = "com.hanaretamae.kaede";
            desktopName = "Kaede Gallery";
            comment = "Offline, read-only gallery for Obsidian Vaults";
            exec = "kaede-gallery";
            icon = "kaede-gallery";
            categories = [
              "Graphics"
              "Viewer"
            ];
            startupWMClass = "com-hanaretamae-kaede-desktop-MainKt";
          };
          kmpDesktopBase = pkgs.stdenv.mkDerivation (finalAttrs: {
            pname = "kaede-gallery-kmp";
            inherit version;
            src = self.outPath;
            sourceRoot = "source/kotlin";

            nativeBuildInputs = with pkgs; [
              gradle
              rustupShim
              jdk17
              cargo
              rustc
              makeWrapper
            ];
            buildInputs = with pkgs; [
              ffmpeg
              fontconfig
              glib
              libGL
              libX11
              libXext
              libXi
              libXrender
              libXtst
              libass
              libjpeg
              libplacebo
              libxkbcommon
              mpv
              stdenv.cc.cc.lib
            ];

            JAVA_HOME = "${pkgs.jdk17}";
            RUSTC = "${pkgs.rustc}/bin/rustc";
            RUSTDOC = "${pkgs.rustc}/bin/rustdoc";
            gradleFlags = [
              "-Dorg.gradle.java.home=${pkgs.jdk17}"
              "-Porg.gradle.java.installations.auto-download=false"
              "-Porg.gradle.java.installations.auto-detect=false"
              "-Pkaede.uniffi.bindgenSource=../uniffi-bindgen-source"
              "--no-configuration-cache"
            ];
            gradleBuildTask = ":desktopApp:createDistributable";
            gradleUpdateTask = finalAttrs.gradleBuildTask;
            enableParallelUpdating = false;

            preBuild = ''
              chmod -R u+w ..
              export HOME="$NIX_BUILD_TOP/home"
              mkdir -p "$HOME" ../.cargo ../uniffi-bindgen-source
              cp -a ${bindgenSource}/. ../uniffi-bindgen-source/
              chmod -R u+w ../uniffi-bindgen-source
              mkdir -p ../uniffi-bindgen-source/.cargo
              cat > ../uniffi-bindgen-source/.cargo/config.toml <<'EOF'
              [source.crates-io]
              replace-with = "vendored-sources"

              [source.vendored-sources]
              directory = "${bindgenVendor}/source-registry-0"

              [net]
              offline = true
              EOF
              cat > ../.cargo/config.toml <<'EOF'
              [source.crates-io]
              replace-with = "vendored-sources"

              [source.vendored-sources]
              directory = "${cargoVendor}/source-registry-0"

              [net]
              offline = true
              EOF
            '';

            installPhase = ''
              runHook preInstall
              app_dir="$out/share/kaede-gallery"
              mkdir -p "$app_dir"
              cp -a desktopApp/build/compose/binaries/main/app/desktopApp/. "$app_dir/"
              rm -f "$app_dir/lib/libapplauncher.so" "$app_dir/lib/app/.jpackage.xml"

              cat > "$app_dir/bin/desktopApp" <<'EOF'
              #!${pkgs.bash}/bin/bash
              set -euo pipefail
              export PATH="${pkgs.lib.makeBinPath [
                pkgs.ffmpeg
                pkgs.mpv
                pkgs.coreutils
                pkgs.glib
              ]}''${PATH:+:$PATH}"
              launcher_path="$(readlink -f -- "$0")"
              app_dir="$(cd -- "$(dirname -- "$launcher_path")/../lib/app" && pwd -P)"
              export LD_LIBRARY_PATH="${pkgs.lib.makeLibraryPath [
                    pkgs.fontconfig
                    pkgs.glib.out
                    pkgs.libGL
                    pkgs.libX11
                    pkgs.libXext
                    pkgs.libXi
                    pkgs.libXrender
                    pkgs.libXtst
                    pkgs.libxkbcommon
                    pkgs.stdenv.cc.cc.lib
                ]}''${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
              exec ${pkgs.jdk17}/bin/java \
                -Djpackage.app-version=${version} \
                -Dcompose.application.resources.dir="$app_dir/resources" \
                -Dcompose.application.configure.swing.globals=true \
                -Dskiko.library.path="$app_dir" \
                -cp "$app_dir/*" \
                com.hanaretamae.kaede.desktop.MainKt "$@"
              EOF
              chmod 755 "$app_dir/bin/desktopApp"
              mkdir -p "$out/bin"
              ln -s "$app_dir/bin/desktopApp" "$out/bin/kaede-gallery"

              install -Dm644 \
                ${self}/kotlin/shared-assets/branding/kaede-gallery-icon.png \
                "$out/share/icons/hicolor/512x512/apps/kaede-gallery.png"
              install -Dm644 \
                ${kmpDesktopItem}/share/applications/com.hanaretamae.kaede.desktop \
                "$out/share/applications/com.hanaretamae.kaede.desktop"
              install -Dm644 ${self}/LICENSE \
                "$out/share/licenses/kaede-gallery/LICENSE"
              install -Dm644 ${self}/THIRD_PARTY_NOTICES.md \
                "$out/share/licenses/kaede-gallery/THIRD_PARTY_NOTICES.md"
              mkdir -p "$out/share/licenses/kaede-gallery/dependencies"
              cp -R ${self}/kotlin/shared-assets/licenses/. \
                "$out/share/licenses/kaede-gallery/dependencies/"
              runHook postInstall
            '';

            meta = {
              description = "Offline, read-only gallery for Obsidian Vaults";
              homepage = "https://github.com/hanaretamae/Kaede-Gallery";
              license = pkgs.lib.licenses.gpl3Plus;
              mainProgram = "kaede-gallery";
              platforms = [
                "x86_64-linux"
                "aarch64-linux"
              ];
            };
          });
          kmpDesktop = kmpDesktopBase.overrideAttrs (old: {
            mitmCache = gradle.fetchDeps {
              pkg = kmpDesktopBase;
              pname = old.pname;
              attrPath = null;
              data = "kotlin/deps.json";
              silent = false;
            };
          });
          aarch64CrossDesktop =
            if system == "x86_64-linux" then
              let
                targetPkgs = mkPkgs "aarch64-linux";
                crossPkgs = pkgs.pkgsCross.aarch64-multiplatform;
                crossRustc = crossPkgs.pkgsBuildHost.rustc;
                skikoRuntimeArm64 = pkgs.fetchurl {
                  url = "https://repo.maven.apache.org/maven2/org/jetbrains/skiko/skiko-awt-runtime-linux-arm64/0.153.0/skiko-awt-runtime-linux-arm64-0.153.0.jar";
                  hash = "sha256-+Jh77caV+q/658qimIVcJaBf1ZoVMXOkN5GKkuGXhkY=";
                };
                crossDesktopBase = kmpDesktopBase.overrideAttrs (old: {
                  pname = "kaede-gallery-kmp-aarch64-cross";
                  nativeBuildInputs = old.nativeBuildInputs ++ [
                    pkgs.cargo
                    crossRustc
                    crossPkgs.stdenv.cc
                    pkgs.python3
                    pkgs.zip
                  ];
                  gradleFlags = old.gradleFlags ++ [
                    "-Pkaede.desktop.linuxArchitecture=aarch64"
                  ];
                  postBuild = (old.postBuild or "") + ''
                    target_dir="$NIX_BUILD_TOP/kaede-gallery-aarch64-target"
                    RUSTC=${crossRustc}/bin/rustc \
                      CC_aarch64_unknown_linux_gnu=${crossPkgs.stdenv.cc}/bin/aarch64-unknown-linux-gnu-gcc \
                      AR_aarch64_unknown_linux_gnu=${crossPkgs.stdenv.cc}/bin/aarch64-unknown-linux-gnu-ar \
                      CARGO_TARGET_AARCH64_UNKNOWN_LINUX_GNU_LINKER=${crossPkgs.stdenv.cc}/bin/aarch64-unknown-linux-gnu-gcc \
                      CARGO_TARGET_DIR="$target_dir" \
                      ${pkgs.cargo}/bin/cargo build \
                        --manifest-path ../crates/gallery-ffi/Cargo.toml \
                        --package gallery-ffi \
                        --release \
                        --locked \
                        --target aarch64-unknown-linux-gnu

                    native_library="$target_dir/aarch64-unknown-linux-gnu/release/libgallery_ffi.so"
                    test -f "$native_library"
                    machine=$(od -An -tu2 -j18 -N2 "$native_library" | tr -d '[:space:]')
                    test "$machine" = 183

                    app_lib="$PWD/desktopApp/build/compose/binaries/main/app/desktopApp/lib/app"
                    mapfile -t rust_jars < <(find "$app_lib" -maxdepth 1 -type f -name 'rust-jvm-*.jar' -print)
                    test "''${#rust_jars[@]}" -eq 1
                    rust_jar="''${rust_jars[0]}"
                    ${pkgs.zip}/bin/zip -q -d "$rust_jar" linux-x86-64/libgallery_ffi.so
                    mkdir -p "$NIX_BUILD_TOP/kaede-gallery-native/linux-aarch64"
                    cp "$native_library" "$NIX_BUILD_TOP/kaede-gallery-native/linux-aarch64/libgallery_ffi.so"
                    ${pkgs.jdk17}/bin/jar --update --file "$rust_jar" \
                      -C "$NIX_BUILD_TOP/kaede-gallery-native" linux-aarch64/libgallery_ffi.so
                    ${pkgs.jdk17}/bin/jar --list --file "$rust_jar" |
                      grep -Fx linux-aarch64/libgallery_ffi.so

                    app_lib="$PWD/desktopApp/build/compose/binaries/main/app/desktopApp/lib/app"
                    rm -f "$app_lib/libskiko-linux-x64.so" "$app_lib/libskiko-linux-x64.so.sha256"
                    mkdir -p "$NIX_BUILD_TOP/kaede-gallery-skiko-arm64"
                    (
                      cd "$NIX_BUILD_TOP/kaede-gallery-skiko-arm64"
                      ${pkgs.jdk17}/bin/jar --extract --file ${skikoRuntimeArm64} \
                        libskiko-linux-arm64.so libskiko-linux-arm64.so.sha256
                    )
                    cp "$NIX_BUILD_TOP/kaede-gallery-skiko-arm64/libskiko-linux-arm64.so" "$app_lib/"
                    cp "$NIX_BUILD_TOP/kaede-gallery-skiko-arm64/libskiko-linux-arm64.so.sha256" "$app_lib/"
                    skiko_machine=$(od -An -tu2 -j18 -N2 "$app_lib/libskiko-linux-arm64.so" |
                      tr -d '[:space:]')
                    test "$skiko_machine" = 183
                  '';
                  installPhase = ''
                    runHook preInstall
                    app_dir="$out/share/kaede-gallery"
                    app_image="$PWD/desktopApp/build/compose/binaries/main/app/desktopApp"
                    mkdir -p "$app_dir"
                    cp -a "$app_image/." "$app_dir/"

                    rm -rf "$app_dir/lib/runtime"
                    mkdir -p "$app_dir/lib/runtime"
                    cp -a ${targetPkgs.temurin-bin-17}/. "$app_dir/lib/runtime/"
                    rm -f "$app_dir/lib/libapplauncher.so" "$app_dir/lib/app/.jpackage.xml"

                    cat > "$app_dir/bin/desktopApp" <<'EOF'
                    #!${targetPkgs.bash}/bin/bash
                    set -euo pipefail
                    export PATH="${targetPkgs.lib.makeBinPath [
                      targetPkgs.ffmpeg
                      targetPkgs.mpv
                      targetPkgs.coreutils
                      targetPkgs.glib
                    ]}''${PATH:+:$PATH}"
                    launcher_path="$(readlink -f -- "$0")"
                    app_dir="$(cd -- "$(dirname -- "$launcher_path")/../lib/app" && pwd -P)"
                    export LD_LIBRARY_PATH="${targetPkgs.lib.makeLibraryPath [
                      targetPkgs.fontconfig
                      targetPkgs.glib.out
                      targetPkgs.libGL
                      targetPkgs.libX11
                      targetPkgs.libXext
                      targetPkgs.libXi
                      targetPkgs.libXrender
                      targetPkgs.libXtst
                      targetPkgs.libxkbcommon
                      targetPkgs.stdenv.cc.cc.lib
                    ]}''${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
                    exec "$app_dir/../runtime/bin/java" \
                      -Djpackage.app-version=${version} \
                      -Dcompose.application.resources.dir="$app_dir/resources" \
                      -Dcompose.application.configure.swing.globals=true \
                      -Dskiko.library.path="$app_dir" \
                      -cp "$app_dir/*" \
                      com.hanaretamae.kaede.desktop.MainKt "$@"
                    EOF
                    chmod 755 "$app_dir/bin/desktopApp"
                    mkdir -p "$out/bin"
                    ln -s "$app_dir/bin/desktopApp" "$out/bin/kaede-gallery"

                    install -Dm644 \
                      ${self}/kotlin/shared-assets/branding/kaede-gallery-icon.png \
                      "$out/share/icons/hicolor/512x512/apps/kaede-gallery.png"
                    install -Dm644 \
                      ${kmpDesktopItem}/share/applications/com.hanaretamae.kaede.desktop \
                      "$out/share/applications/com.hanaretamae.kaede.desktop"
                    install -Dm644 ${self}/LICENSE \
                      "$out/share/licenses/kaede-gallery/LICENSE"
                    install -Dm644 ${self}/THIRD_PARTY_NOTICES.md \
                      "$out/share/licenses/kaede-gallery/THIRD_PARTY_NOTICES.md"
                    mkdir -p "$out/share/licenses/kaede-gallery/dependencies"
                    cp -R ${self}/kotlin/shared-assets/licenses/. \
                      "$out/share/licenses/kaede-gallery/dependencies/"

                    so_count=0
                    while IFS= read -r elf; do
                      machine=$(od -An -tu2 -j18 -N2 "$elf" | tr -d '[:space:]')
                      test "$machine" = 183
                      so_count=$((so_count + 1))
                    done < <(find -L "$app_dir" -type f -name '*.so' -print)
                    test "$so_count" -gt 0
                    runHook postInstall
                  '';
                  meta = old.meta // {
                    description = "AArch64 Linux Kaede Gallery cross-built on x86_64";
                    platforms = [ "x86_64-linux" ];
                  };
                });
              in
              crossDesktopBase.overrideAttrs (old: {
                mitmCache = gradle.fetchDeps {
                  pkg = crossDesktopBase;
                  pname = old.pname;
                  attrPath = null;
                  data = "kotlin/deps.json";
                  silent = false;
                };
              })
            else
              null;
        in
        {
          kmpDesktop = kmpDesktop;
          default = kmpDesktop;
        }
        // pkgs.lib.optionalAttrs (system == "x86_64-linux") {
          aarch64Cross = aarch64CrossDesktop;
        }
      );

      nixosModules.default = { pkgs, ... }: {
        environment.systemPackages = [ self.packages.${pkgs.stdenv.hostPlatform.system}.default ];
      };

      homeManagerModules.default = { pkgs, ... }: {
        home.packages = [ self.packages.${pkgs.stdenv.hostPlatform.system}.default ];
      };

      devShells = forAllSystems (
        system:
        let
          pkgs = mkPkgs system;
          androidSdk = pkgs.androidenv.composeAndroidPackages {
            platformVersions = [
              "35"
              "36"
              "37"
              "37.1"
            ];
            buildToolsVersions = [ "37.0.0" ];
            includeCmake = true;
            cmakeVersions = [ "3.22.1" ];
            includeNDK = true;
            ndkVersion = "28.2.13676358";
            includeEmulator = false;
            includeSystemImages = false;
            includeSources = false;
            includeExtras = [ ];
            abiVersions = [
              "arm64-v8a"
              "x86_64"
            ];
          };
        in
        {
          default = pkgs.mkShell {
            packages =
              (with pkgs; [
                rustc
                cargo
                cargo-about
                rustfmt
                clippy
                rustup
                cargo-deny
                python3
                git
                github-cli
                keepassxc
                xdotool
                wtype
                xwd

                ffmpeg
                libass
                libplacebo
                mpv
                libjpeg
                pkg-config
                stdenv.cc
                cmake
                ninja
                clang
                gtk3
                glib
                libGL
                jdk17
              ])
              ++ [ androidSdk.androidsdk ];
            JAVA_HOME = "${pkgs.jdk17}";
            RUSTC = "${pkgs.rustc}/bin/rustc";
            RUSTDOC = "${pkgs.rustc}/bin/rustdoc";
            ANDROID_HOME = "${androidSdk.androidsdk}/libexec/android-sdk";
            ANDROID_SDK_ROOT = "${androidSdk.androidsdk}/libexec/android-sdk";
            ANDROID_NDK_HOME = "${androidSdk.androidsdk}/libexec/android-sdk/ndk/28.2.13676358";

            GSETTINGS_SCHEMA_DIR = "${pkgs.gtk3}/share/gsettings-schemas/gtk+3-${pkgs.gtk3.version}/glib-2.0/schemas";
            shellHook = ''
              export LD_LIBRARY_PATH="${
                pkgs.lib.makeLibraryPath [
                  pkgs.libGL
                  pkgs.glib.out
                  pkgs.libx11
                  pkgs.fontconfig
                  pkgs.stdenv.cc.cc.lib
                ]
              }''${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
            '';
          };
        }
      );
    };
}
