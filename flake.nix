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
      version = "1.0.3";
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
      packages.x86_64-linux.default =
        let
          pkgs = mkPkgs "x86_64-linux";
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
            name = "kaede-gallery";
            desktopName = "Kaede Gallery";
            comment = "Offline, read-only gallery for Obsidian Vaults";
            exec = "vault_gallery";
            icon = "applications-graphics";
            categories = [
              "Graphics"
              "Viewer"
            ];
            startupWMClass = "com.example.vault_gallery";
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
            mpv
          ];
          FLUTTER_SUPPRESS_ANALYTICS = "true";
          DART_SUPPRESS_ANALYTICS = "true";
          postInstall = ''
            install -Dm644 \
              ${desktopItem}/share/applications/kaede-gallery.desktop \
              $out/share/applications/kaede-gallery.desktop
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
            license = pkgs.lib.licenses.mit;
            mainProgram = "vault_gallery";
            platforms = [ "x86_64-linux" ];
          };
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
                rustfmt
                clippy
                rustup
                cargo-deny
                git
                github-cli
                keepassxc
                flutter
                ffmpeg
                libass
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
