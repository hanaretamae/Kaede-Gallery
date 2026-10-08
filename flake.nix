{
  description = "Kaede Gallery packages and development environment";

  inputs.nixpkgs.url = "https://channels.nixos.org/nixos-unstable/nixexprs.tar.zst";

  outputs =
    { self, nixpkgs }:
    let
      systems = [
        "x86_64-linux"
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
              mkdir -p "$out/share/kaede-gallery"
              cp -a desktopApp/build/compose/binaries/main/app/desktopApp/. \
                "$out/share/kaede-gallery/"
              makeWrapper "$out/share/kaede-gallery/bin/desktopApp" "$out/bin/kaede-gallery" \
                --prefix LD_LIBRARY_PATH : "${
                  pkgs.lib.makeLibraryPath [
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
                  ]
                }" \
                --prefix PATH : "${
                  pkgs.lib.makeBinPath [
                    pkgs.ffmpeg
                    pkgs.mpv
                  ]
                }"
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
        in
        {
          kmpDesktop = kmpDesktop;
          default = kmpDesktop;
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
