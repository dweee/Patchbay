{
  description = "Patchbay Mod Loader (BML): loads mods into Bitwig Studio";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-26.05";
    home-manager = {
      url = "github:nix-community/home-manager/release-26.05";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    nix-darwin = {
      url = "github:nix-darwin/nix-darwin/nix-darwin-26.05";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs =
    {
      self,
      nixpkgs,
      home-manager,
      nix-darwin,
      ...
    }:
    let
      systems = [
        "x86_64-linux"
        "aarch64-linux"
        "x86_64-darwin"
        "aarch64-darwin"
      ];
      forAllSystems = nixpkgs.lib.genAttrs systems;
      pkgsFor = system: import nixpkgs { inherit system; };
      # The module files, with this flake's package as the default (pkgs.patchbay when the overlay
      # is applied), so a configuration's own nixpkgs doesn't rebuild it.
      withPackage =
        name: file:
        { lib, pkgs, ... }:
        {
          key = "patchbay-flake#${name}";
          _file = "${self.outPath}/flake.nix#${name}";
          imports = [ file ];
          config.programs.patchbay.package = lib.mkDefault (
            pkgs.patchbay or self.packages.${pkgs.stdenv.hostPlatform.system}.default
          );
        };
    in
    {
      packages = forAllSystems (
        system:
        let
          pkgs = pkgsFor system;
        in
        {
          patchbay = pkgs.callPackage ./nix/package.nix { };
          default = self.packages.${system}.patchbay;
        }
      );
      apps = forAllSystems (system: {
        default = {
          type = "app";
          program = "${self.packages.${system}.default}/bin/patchbay";
          meta.description = "Patchbay window (host build of BML)";
        };
      });
      devShells = forAllSystems (
        system:
        let
          pkgs = pkgsFor system;
        in
        {
          default = pkgs.mkShell {
            packages = [
              pkgs.jdk25
              pkgs.python3
              pkgs.stdenv.cc
              pkgs.nodejs
              pkgs.nixfmt
            ]
            # loader/build.py --host joins the universal agent with lipo; only lipo, so cctools'
            # other tools don't shadow the compiler's
            ++ pkgs.lib.optional pkgs.stdenv.hostPlatform.isDarwin (
              pkgs.runCommand "lipo" { } ''
                mkdir -p "$out/bin"
                ln -s ${pkgs.lib.getExe' pkgs.cctools "lipo"} "$out/bin/lipo"
              ''
            );
            JAVA_HOME = pkgs.jdk25;
            BML_JDK_HOME = pkgs.jdk25;
          };
        }
      );
      formatter = forAllSystems (system: (pkgsFor system).nixfmt);
      overlays.default = final: _prev: {
        patchbay = final.callPackage ./nix/package.nix { };
      };
      nixosModules = {
        patchbay = withPackage "nixos" ./nix/modules/system.nix;
        default = self.nixosModules.patchbay;
      };
      darwinModules = {
        patchbay = withPackage "darwin" ./nix/modules/system.nix;
        default = self.darwinModules.patchbay;
      };
      homeManagerModules = {
        patchbay = withPackage "home-manager" ./nix/modules/home-manager.nix;
        default = self.homeManagerModules.patchbay;
      };
      checks = forAllSystems (
        system:
        let
          pkgs = pkgsFor system;
          lib = nixpkgs.lib;
          isDarwin = pkgs.stdenv.hostPlatform.isDarwin;
          package = self.packages.${system}.default;
          # Stand-ins for Bitwig from another flake, in audio.nix's two layouts: the package itself
          # (libexec is the install folder; bin/bitwig-studio links into it) and its bubblewrap
          # wrapper (bin/bitwig-studio a copied script; bin/bitwig-studio-unwrapped links to the first).
          bitwigPlain = pkgs.runCommand "bitwig-studio-check-unwrapped" { } ''
            mkdir -p "$out/bin" "$out/libexec/bin" "$out/libexec/lib/jre/bin"
            printf '#!/bin/sh\nexit 0\n' >"$out/libexec/bitwig-studio"
            chmod +x "$out/libexec/bitwig-studio"
            echo jar >"$out/libexec/bin/bitwig.jar"
            ln -s ${pkgs.jdk25}/bin/java "$out/libexec/lib/jre/bin/java"
            echo 'MODULES="java.base java.desktop"' >"$out/libexec/lib/jre/release"
            ln -s "$out/libexec/bitwig-studio" "$out/bin/bitwig-studio"
          '';
          bitwigWrapped = pkgs.runCommand "bitwig-studio-check" { } ''
            mkdir -p "$out/bin"
            printf '#!/bin/sh\nexec %s "$@"\n' ${bitwigPlain}/bin/bitwig-studio >"$out/bin/bitwig-studio"
            chmod +x "$out/bin/bitwig-studio"
            ln -s ${bitwigPlain}/bin/bitwig-studio "$out/bin/bitwig-studio-unwrapped"
          '';
          fixtureMod =
            version:
            pkgs.writeTextDir "mod.json" (
              builtins.toJSON {
                manifest_version = 2;
                mod = {
                  id = "fixture";
                  name = "Fixture";
                  inherit version;
                  author = "Check";
                  description = "Module check";
                };
                built_against = {
                  bitwig_version = "6.1.4";
                  platform = "linux";
                  architecture = "x64";
                };
                targets = { };
                hooks = [ ];
                trace_targets = [
                  {
                    id = "fixture";
                    class = "example.Helper";
                    method = "run";
                    descriptor = "()V";
                  }
                ];
              }
            );
          fixtureMap = pkgs.writeTextDir "map.json" "{}";
          # Linux takes Bitwig from a package, macOS from an app bundle outside Nix.
          bitwigSettings =
            if isDarwin then
              { bitwigApp = "/Applications/Bitwig Studio.app"; }
            else
              { bitwigPackage = bitwigWrapped; };
          bitwigExe =
            if isDarwin then
              "/Applications/Bitwig Studio.app/Contents/MacOS/BitwigStudio"
            else
              "${bitwigWrapped}/bin/bitwig-studio";
          settings = bitwigSettings // {
            enable = true;
            java = pkgs.jdk25;
          };
          homeDirectory = if isDarwin then "/Users/patchbay-check" else "/home/patchbay-check";
          homeFor =
            extra:
            (home-manager.lib.homeManagerConfiguration {
              inherit pkgs;
              modules = [
                self.homeManagerModules.default
                {
                  home.username = "patchbay-check";
                  home.homeDirectory = homeDirectory;
                  home.stateVersion = "26.05";
                  programs.patchbay = settings // extra;
                }
              ];
            }).config;
          hc = homeFor {
            mods.fixture = fixtureMod "1.0";
            mapPacks."linux-6.1.4" = fixtureMap;
          };
          # the next activations: the mod's source changed and the map pack is gone; then a mod
          # whose folder the user made
          hcChanged = homeFor { mods.fixture = fixtureMod "2.0"; };
          hcCollides = homeFor { mods.mine = fixtureMod "1.0"; };
          hcPlain = homeFor {
            bitwigPackage = bitwigPlain;
            bitwigApp = null;
          };
          hcExplicit = homeFor {
            bitwigPackage = bitwigWrapped;
            bitwigApp = null;
            bitwigHome = "/opt/bitwig-studio";
          };
          bothRefused =
            !(builtins.tryEval
              (home-manager.lib.homeManagerConfiguration {
                inherit pkgs;
                modules = [
                  self.homeManagerModules.default
                  {
                    home.username = "patchbay-check";
                    home.homeDirectory = homeDirectory;
                    home.stateVersion = "26.05";
                    programs.patchbay = settings // {
                      bitwigPackage = bitwigWrapped;
                      bitwigApp = "/Applications/Bitwig Studio.app";
                    };
                  }
                ];
              }).activationPackage.drvPath
            ).success;
          host =
            if isDarwin then
              nix-darwin.lib.darwinSystem {
                modules = [
                  self.darwinModules.default
                  {
                    nixpkgs.hostPlatform = system;
                    system.stateVersion = 6;
                    programs.patchbay = settings;
                  }
                ];
              }
            else
              lib.nixosSystem {
                inherit system;
                modules = [
                  self.nixosModules.default
                  {
                    system.stateVersion = "26.05";
                    boot.isContainer = true;
                    fileSystems."/" = {
                      device = "none";
                      fsType = "tmpfs";
                    };
                    programs.patchbay = settings;
                  }
                ];
              };
          root = "${homeDirectory}/.local/share/bml";
          vars = hc.home.sessionVariables;
          hostVars =
            if isDarwin then host.config.environment.variables else host.config.environment.sessionVariables;
          java = "${pkgs.jdk25}/bin/java";
          blocks = c: [
            c.home.activation.patchbayCheck.data
            c.home.activation.patchbay.data
          ];
          expectations = {
            "home: assertions hold" = lib.all (a: a.assertion) hc.assertions;
            "home: no warnings" = hc.warnings == [ ];
            "home: bitwigPackage and bitwigApp refused together" = bothRefused;
            "home: the flake's own package" = hc.programs.patchbay.package.drvPath == package.drvPath;
            "home: package installed" = lib.elem hc.programs.patchbay.package hc.home.packages;
            "home: bitwig installed (Linux)" = isDarwin || lib.elem bitwigWrapped hc.home.packages;
            "home: BML_ROOT" = vars.BML_ROOT == root;
            "home: BML_JAVA" = vars.BML_JAVA == java;
            "home: BML_BITWIG_EXECUTABLE" = vars.BML_BITWIG_EXECUTABLE == bitwigExe;
            "home: BML_BITWIG_HOME found in the package (Linux), none for an app" =
              if isDarwin then
                !(vars ? BML_BITWIG_HOME)
              else
                lib.hasPrefix builtins.storeDir vars.BML_BITWIG_HOME;
            "home: BML_BITWIG_HOME of the plain layout" =
              lib.hasPrefix builtins.storeDir hcPlain.home.sessionVariables.BML_BITWIG_HOME;
            "home: BML_BITWIG_EXECUTABLE of the plain layout" =
              hcPlain.home.sessionVariables.BML_BITWIG_EXECUTABLE == "${bitwigPlain}/bin/bitwig-studio";
            "home: bitwigHome set by hand" =
              hcExplicit.home.sessionVariables.BML_BITWIG_HOME == "/opt/bitwig-studio";
            "home: mods and map packs are not links" =
              !(lib.any (f: lib.hasPrefix "${root}/" f.target || lib.hasPrefix ".local/share/bml/" f.target) (
                lib.attrValues hc.home.file
              ));
            "home: activation copies the mod and the map pack" =
              let
                plain = builtins.unsafeDiscardStringContext;
                data = plain (lib.replaceStrings [ "'" ] [ "" ] hc.home.activation.patchbay.data);
              in
              lib.hasInfix "run cp -RL" data
              && lib.hasInfix (plain "[mods/fixture]=${fixtureMod "1.0"}") data
              && lib.hasInfix (plain "[maps/linux-6.1.4]=${fixtureMap}") data;
            "home: bml.properties unmanaged" =
              !(lib.any (lib.hasInfix "bml.properties") (lib.mapAttrsToList (_: f: f.target) hc.home.file));
            "host: assertions hold" = lib.all (a: a.assertion) host.config.assertions;
            "host: no warnings" = host.config.warnings == [ ];
            "host: the flake's own package" = host.config.programs.patchbay.package.drvPath == package.drvPath;
            "host: package installed" =
              lib.elem host.config.programs.patchbay.package host.config.environment.systemPackages;
            "host: bitwig installed (Linux)" =
              isDarwin || lib.elem bitwigWrapped host.config.environment.systemPackages;
            "host: BML_JAVA" = hostVars.BML_JAVA == java;
            "host: BML_BITWIG_EXECUTABLE" = hostVars.BML_BITWIG_EXECUTABLE == bitwigExe;
            "host: BML_BITWIG_HOME (Linux)" = isDarwin || hostVars.BML_BITWIG_HOME == vars.BML_BITWIG_HOME;
            "host: no BML_ROOT" = !(hostVars ? BML_ROOT);
          };
          failed = lib.attrNames (lib.filterAttrs (_: ok: !ok) expectations);
        in
        {
          inherit package;
          modules =
            if failed != [ ] then
              throw "patchbay module checks failed: ${lib.concatStringsSep "; " failed}"
            else
              pkgs.runCommand "patchbay-module-evaluation" { } ''
                # both Bitwig layouts give the unwrapped install folder
                test "$(readlink -f ${vars.BML_BITWIG_HOME or "${bitwigPlain}/libexec"})" = ${bitwigPlain}/libexec
                test "$(readlink -f ${hcPlain.home.sessionVariables.BML_BITWIG_HOME})" = ${bitwigPlain}/libexec
                touch "$out"
              '';

          # The home-manager activation blocks, run as home-manager runs them (bash, set -eu
          # -o pipefail, its run/verboseEcho/errorEcho) on a scratch root, and the copies read by
          # the real bml-control.jar.
          activation =
            pkgs.runCommand "patchbay-home-activation"
              {
                nativeBuildInputs = [ pkgs.jdk25 ];
              }
              ''
                export HOME="$TMPDIR/home" XDG_CONFIG_HOME="$TMPDIR/home/.config"
                scratch="$HOME/bml"
                mkdir -p "$HOME"
                cp -R ${package}/share/bml "$scratch"
                chmod -R u+w "$scratch"
                activate() {
                  for f; do sed "s|${root}|$scratch|g" "$f"; done >"$TMPDIR/blocks.sh"
                  ${pkgs.bash}/bin/bash -c '
                    set -eu -o pipefail
                    run() { if [[ -v DRY_RUN ]]; then echo "$@"; else "$@"; fi; }
                    verboseEcho() { :; }
                    errorEcho() { echo "$*" >&2; }
                    source "$1"
                  ' activate "$TMPDIR/blocks.sh"
                }
                status() {
                  java -Djava.awt.headless=true -Duser.home="$HOME" -Dbml.root="$scratch" -jar "$scratch/loader/app/bml-control.jar" --status \
                    | tee "$TMPDIR/status.txt"
                }

                activate ${lib.concatStringsSep " " (map (pkgs.writeText "block") (blocks hc))}
                test -d "$scratch/mods/fixture" && test ! -L "$scratch/mods/fixture"
                test -d "$scratch/maps/linux-6.1.4" && test ! -L "$scratch/maps/linux-6.1.4"
                test -z "$(find "$scratch/mods" "$scratch/maps" -type l)"
                test -w "$scratch/mods/fixture/mod.json"
                grep -q '"version":"1.0"' "$scratch/mods/fixture/mod.json"
                test "$(cut -f1 "$scratch/.hm-managed" | tr '\n' ' ')" = "maps/linux-6.1.4 mods/fixture "
                test ! -e "$scratch/.hm-staging"
                status
                grep -q '^mod=fixture ' "$TMPDIR/status.txt"
                grep -q '^installed_packages=1 blocking_issues=0$' "$TMPDIR/status.txt"
                if grep -q 'symbolic link' "$TMPDIR/status.txt"; then exit 1; fi

                # what BML says of a linked mod, which is why the folders are copied
                ln -s ${fixtureMod "1.0"} "$scratch/mods/linked"
                status
                grep -q 'symbolic link' "$TMPDIR/status.txt"
                rm "$scratch/mods/linked"

                # the user's own folders, a dry run, then the source changed and the map pack dropped
                mkdir -p "$scratch/mods/mine" "$scratch/maps/6.1.4"
                echo mine >"$scratch/mods/mine/mod.json"
                echo mine >"$scratch/maps/6.1.4/map.json"
                before=$(find "$scratch/mods" "$scratch/maps" | sort; cat "$scratch/.hm-managed")
                DRY_RUN=1 activate ${lib.concatStringsSep " " (map (pkgs.writeText "block") (blocks hcChanged))}
                test "$(find "$scratch/mods" "$scratch/maps" | sort; cat "$scratch/.hm-managed")" = "$before"
                activate ${lib.concatStringsSep " " (map (pkgs.writeText "block") (blocks hcChanged))}
                grep -q '"version":"2.0"' "$scratch/mods/fixture/mod.json"
                test ! -e "$scratch/maps/linux-6.1.4"
                test "$(cat "$scratch/mods/mine/mod.json")" = mine
                test "$(cat "$scratch/maps/6.1.4/map.json")" = mine
                test "$(cut -f1 "$scratch/.hm-managed")" = mods/fixture
                # unchanged: left as it is
                touch "$scratch/mods/fixture/kept"
                activate ${lib.concatStringsSep " " (map (pkgs.writeText "block") (blocks hcChanged))}
                test -e "$scratch/mods/fixture/kept"

                # a declared mod whose folder the user made stops the activation before anything is written
                rc=0
                activate ${lib.concatStringsSep " " (map (pkgs.writeText "block") (blocks hcCollides))} 2>"$TMPDIR/err.txt" || rc=$?
                cat "$TMPDIR/err.txt"
                test "$rc" != 0
                grep -q 'was not copied there by home-manager' "$TMPDIR/err.txt"
                test "$(cat "$scratch/mods/mine/mod.json")" = mine
                test -e "$scratch/mods/fixture/kept"
                touch "$out"
              '';
        }
        // lib.optionalAttrs (!isDarwin) {
          # BML_BITWIG_HOME read by platform.sh and the Java side, for the wrapper layout: Bitwig's
          # files from the unwrapped install folder, the wrapper still the executable started.
          bitwig-home =
            pkgs.runCommand "patchbay-bitwig-home"
              {
                nativeBuildInputs = [ pkgs.jdk25 ];
              }
              ''
                share=${package}/share/bml
                plain=${bitwigPlain}/libexec
                export HOME="$TMPDIR/home" XDG_CONFIG_HOME="$TMPDIR/home/.config"
                export BML_BITWIG_EXECUTABLE=${bitwigExe} BML_BITWIG_HOME=${vars.BML_BITWIG_HOME}
                mkdir -p "$TMPDIR/other/bin" "$HOME"
                printf '#!/bin/sh\n' >"$TMPDIR/other/bitwig-studio"
                chmod +x "$TMPDIR/other/bitwig-studio"
                . "$share/loader/platform.sh"
                test "$(bml_bitwig_exe "")" = ${bitwigExe}
                test "$(bml_bitwig_bin "$(bml_real_path ${bitwigExe})")" = "$plain/bin"
                test "$(bml_bitwig_java)" = "$plain/lib/jre/bin/java"
                test "$(bml_bitwig_java ${bitwigExe})" = "$plain/lib/jre/bin/java"
                test "$(bml_find_java "$share" "$plain/bin")" = "$plain/bin/../lib/jre/bin/java"
                # another Bitwig than the default: its own folders
                test "$(bml_bitwig_bin "$TMPDIR/other/bitwig-studio")" = "$TMPDIR/other/bin"
                # without BML_BITWIG_HOME: beside the wrapper, as before (the preparer then says bitwig.jar is missing)
                test "$(BML_BITWIG_HOME= bml_bitwig_bin ${bitwigExe})" = ${bitwigWrapped}/bin/bin

                mkdir -p probe/dev/bitwigre/bml
                cat >probe/dev/bitwigre/bml/Probe.java <<'EOF'
                package dev.bitwigre.bml;
                import java.nio.file.Path;
                public class Probe {
                    public static void main(String[] a) throws Exception {
                        BmlConfig c = new BmlConfig(new BmlPaths(Path.of(a[0])));
                        c.load();
                        var bin = JavaPreparer.class.getDeclaredMethod("defaultBin");
                        bin.setAccessible(true);
                        System.out.println(c.bitwigExecutable());
                        System.out.println(BmlConfig.jarFolder(c.bitwigExecutable()));
                        System.out.println(bin.invoke(null));
                        System.out.println(BmlConfig.jarFolder(Path.of(a[1])));
                    }
                }
                EOF
                javac -cp "$share/loader/app/bml-control.jar" -d probe probe/dev/bitwigre/bml/Probe.java
                java -Duser.home="$HOME" -cp "probe:$share/loader/app/bml-control.jar" dev.bitwigre.bml.Probe "$TMPDIR/root" "$TMPDIR/other/bitwig-studio" \
                  | tee probe.txt
                printf '%s\n' ${bitwigExe} "$plain/bin" "$plain/bin" "$TMPDIR/other/bin" | diff - probe.txt
                touch "$out"
              '';
        }
      );
    };
}
