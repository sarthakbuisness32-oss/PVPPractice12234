# PvPPractice (starter rebuild)

Target: Paper 1.21.11, Java 21. NPC rendering is provided by FancyNpcs.

## Dependencies

1. Paper 1.21.11 running on Java 21.
2. A FancyNpcs build compatible with both Paper 1.21.11 and Java 21. FancyNpcs 2.9.2's API documentation says that version adds 1.21.11 support; verify the actual downloadable plugin build is Java 21-compatible before using it.

## Build without installing Java/Maven locally

Upload this project's files to your GitHub repository (the `.github/workflows/build.yml` file must be in the repository root). Open **Actions**, select **Build PvPPractice**, run the workflow, then download the `PvPPractice-JAR` artifact.

## Install

Put `PvPPractice.jar` and the compatible `FancyNpcs.jar` in the server's `plugins` folder, then restart the server.

## Commands

- `/interiobot help`
- `/interiobot mass_spawn` — creates 10 NPCs by default
- `/interiobot mass_spawn <amount>` — maximum is configurable in `config.yml`
- `/interiobot spawn <name> [skin]`
- `/interiobot remove <name>`
- `/interiobot removeall`
- `/interiobot list`
- `/interiobot skin <name> <playerSkinName>`
- `/interiobot difficulty <name> <easy|normal|hard>` — saves the selected level for future AI work; it does not affect combat yet

## Honest current limitations

This starter build creates player-looking packet NPCs. FancyNpcs NPCs are not real server-side player entities, so this build does **not** provide movement, pathfinding, attacks, taking damage, or VexBot-style combat AI yet. Those features require a separate AI/actual-entity integration stage. The follow/duel/kit/setspawn/reload commands are planned but not implemented in this build.
