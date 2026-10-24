# Verification matrix

This file distinguishes completed tests from planned checks. It is not a claim of live-server deployment.

## Completed

- Read exact Minecraft 1.20.1 vanilla reference implementations and Forge 47.4.13 source patches for login ordering, tracked entities, player info, status, query, `/list`, suggestions, game modes and player save data.
- 94 JUnit config tests passed on Java 17 / Gson 2.10 / JUnit Platform 1.10.2. Tests cover strict parsing, duplicate keys/UUIDs, empty defaults, optional observer mode, file permissions, no overwrite and immutable restart-only snapshots.

- Full Gradle `test build` passed: 94 tests, zero failures/skips; production reobfuscation and refmap generated.
- Isolated dedicated Forge 47.4.13 server booted with the production JAR and strict mixin injection counts; modern status ping and console `/list` succeeded, Query listener bound only 127.0.0.1; clean shutdown/world save.
- Initial runtime caught a Mixin 0.8.5 array-clone transformation bug; replaced the array clone with `Arrays.copyOf`, rebuilt and verified successful boot. Compile alone was not used as a runtime pass.

## Required before release

- Visible and configured-hidden protocol clients without this mod: hidden first-login never reaches visible Tab/tracking/join, including visible client joining later; hidden disconnect and latency updates.
- Modern status count/sample, legacy ping, Query count/names; console real list versus player-filtered list and name completion.
- Restart with UUID retained/removed and observer mode disabled; uninstall fallback verifies original saved game mode.
- Respawn/dimension changes and visible-player regression.
- Other relevant public presence surfaces audited and tested. The inspected TFG server has no active Minecraft Discord bridge; retained Discord integration configuration is inactive. External operator/RCON automation intentionally sees authoritative players.

The live TFG server is not an integration test target and must not be restarted for these checks.
