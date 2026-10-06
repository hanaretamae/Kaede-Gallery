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
      version = "1.6.2";
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
      packages = forAllSystems (system: {
        default =
        let
          pkgs = mkPkgs system;
          cargoVendor = pkgs.rustPlatform.fetchCargoVendor {
            src = self;
            cargoLock.lockFile = ./Cargo.lock;
            hash = "sha256-VqOvEKwCS3YIqZCJsx5/A8sPzjKjzZ54ncv3Ew2j1a8=";
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
          desktopItem = pkgs.makeDesktopItem {
            name = "com.hanaretamae.kaede_gallery";
            desktopName = "Kaede Gallery";
            comment = "Offline, read-only gallery for Obsidian Vaults";
            exec = "kaede_gallery";
            icon = "kaede-gallery";
            categories = [
              "Graphics"
              "Viewer"
            ];
            startupWMClass = "com.hanaretamae.kaede_gallery";
          };
        in
        pkgs.flutterPackages.stable.buildFlutterApplication (finalAttrs: {
          pname = "kaede-gallery";
          inherit version;
          src = self.outPath;
          sourceRoot = "source/app";
          autoPubspecLock = self + "/app/pubspec.lock";
          dontUseCmakeConfigure = true;
          nativeBuildInputs = [
            rustupShim
          ]
          ++ (with pkgs; [
            cargo
            cmake
            ninja
            pkg-config
            rustc
            stdenv.cc
          ]);
          buildInputs = with pkgs; [
            ffmpeg
            gtk3
            libGL
            libass
            libjpeg
            libplacebo
            mpv
          ];
          FLUTTER_SUPPRESS_ANALYTICS = "true";
          DART_SUPPRESS_ANALYTICS = "true";
          postInstall = ''
            install -Dm644 \
              linux/kaede-gallery-512.png \
              $out/share/icons/hicolor/512x512/apps/kaede-gallery.png
            install -Dm644 \
              ${desktopItem}/share/applications/com.hanaretamae.kaede_gallery.desktop \
              $out/share/applications/com.hanaretamae.kaede_gallery.desktop
            install -Dm644 \
              ${self}/LICENSE \
              $out/share/licenses/kaede-gallery/LICENSE
            install -Dm644 \
              ${self}/THIRD_PARTY_NOTICES.md \
              $out/share/licenses/kaede-gallery/THIRD_PARTY_NOTICES.md
            cp -R assets/licenses/. $out/share/licenses/kaede-gallery/
          '';
          preBuild = ''
            export HOME="$NIX_BUILD_TOP/home"
            mkdir -p "$HOME"
            export RUSTC="${pkgs.rustc}/bin/rustc"
            mkdir -p "$NIX_BUILD_TOP/source/app/.cargo"
            cat > "$NIX_BUILD_TOP/source/app/.cargo/config.toml" <<'EOF'
            [source.crates-io]
            replace-with = "vendored-sources"

            [source.vendored-sources]
            directory = "${cargoVendor}/source-registry-0"

            [net]
            offline = true
            EOF
          '';
          meta = {
            description = "Offline, read-only gallery for Obsidian Vaults";
            homepage = "https://github.com/hanaretamae/Kaede-Gallery";
            license = pkgs.lib.licenses.gpl3Plus;
            mainProgram = "kaede_gallery";
            platforms = [ "x86_64-linux" "aarch64-linux" ];
          };
        });
      });

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
            ];
            buildToolsVersions = [ "36.0.0" ];
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
                flutter
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
            ANDROID_HOME = "${androidSdk.androidsdk}/libexec/android-sdk";
            ANDROID_SDK_ROOT = "${androidSdk.androidsdk}/libexec/android-sdk";
            ANDROID_NDK_HOME = "${androidSdk.androidsdk}/libexec/android-sdk/ndk/28.2.13676358";
            FLUTTER_SUPPRESS_ANALYTICS = "true";
            DART_SUPPRESS_ANALYTICS = "true";
            GSETTINGS_SCHEMA_DIR = "${pkgs.gtk3}/share/gsettings-schemas/gtk+3-${pkgs.gtk3.version}/glib-2.0/schemas";
          };
        }
      );
    };
}
