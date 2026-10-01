{
  description = "Development environment for vault-gallery";

  inputs.nixpkgs.url = "https://channels.nixos.org/nixos-unstable/nixexprs.tar.zst";

  outputs = { self, nixpkgs }:
    let
      systems = [ "x86_64-linux" "aarch64-linux" ];
      forAllSystems = nixpkgs.lib.genAttrs systems;
    in
    {
      devShells = forAllSystems (system:
        let pkgs = import nixpkgs { inherit system; };
        in {
          default = pkgs.mkShell {
            packages = with pkgs; [
              rustc
              cargo
              rustfmt
              clippy
              rustup
              cargo-deny
              flutter
              pkg-config
              stdenv.cc
              cmake
              ninja
              clang
              gtk3
              glib
              libGL
              python3
            ];
            FLUTTER_SUPPRESS_ANALYTICS = "true";
            DART_SUPPRESS_ANALYTICS = "true";
          };
        });
    };
}
