# Changelog

Changes to Patchbay Mod Loader, newest first. Mods have their own notes on the [Mods releases page](https://github.com/shaboogen-dev/Mods/releases). Versions before 0.36.0-dev.13 were test builds; their notes are on the [releases page](https://github.com/shaboogen-dev/Patchbay/releases).

## 0.36.0-dev.16 (15 Nov 2026)

### Fixed

- **macOS:** Load no longer fails with `Invalid map package directory: …/maps/.DS_Store`. Finder creates that hidden file when you open the `maps` folder (for example with **Open Folder** on the Maps page). Patchbay now ignores hidden files and system files (`.DS_Store`, `._` files, `Thumbs.db`, `desktop.ini`) in `maps`, when preparing, on the Maps page and in **Import Map…**. A changed `.DS_Store` also no longer makes Patchbay prepare again.
- **Windows:** with many mods ticked, preparing could fail with `CreateProcess error=206` (the command line got too long). Patchbay now passes the list in a file instead.
- The map tools in `map-tools/` skip the same hidden files.

## 0.36.0-dev.14 (14 Nov 2026)

### Fixed

- **Windows:** Patchbay starts even when Windows' own folders are missing from PATH. Before, it stopped with `'findstr' is not recognized` and then said no Java was found.

## 0.36.0-dev.13 (14 Nov 2026)

### Added

- **Add Mod…** on the Mods page installs mod ZIPs, several at once (also Ctrl+O / Cmd+O, or drag ZIPs onto the window). Each ZIP is checked before anything is written, and a bad one is refused with the reason. Adding a newer version of an installed mod asks first, and keeps its tick and settings.
- **Disable Mod:** right-click a mod to set it aside without deleting it. **Show → Disabled** lists it again.

### Changed

- **Patchbay ships without mods.** The download holds the loader and the ten frameworks. Each mod is its own ZIP on the Mods releases page, along with a ZIP of every mod.
