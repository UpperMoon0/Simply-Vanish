# Verification matrix

Tests run on Java 17 and exact Forge 1.20.1-47.4.13, using a disposable loopback-only cloud fixture. No live TFG world, authentication config, deployment or restart was touched.

## Passed

- 94 Gradle/JUnit config tests, zero failures/skips: strict JSON/UUID parsing; duplicate keys and UUIDs; empty defaults; optional observer mode; file permissions; no overwrites; immutable startup-only snapshots.
- Production build, reobfuscation, packaged refmap; dedicated-server boot with strict mixin injection counts and clean shutdown.
- Three unmodified vanilla-protocol clients (two visible, one hidden), no client-side mod: initial and later viewers, Tab/player-info, actual visible entity tracking control, hidden entity tracking, join/leave, and 30-second latency updates.
- Modern status count/sample with real synthetic visible names, legacy 1.3/1.4+ ping, GS4 full Query count/names: two public players while console `/list` showed three.
- Player-side vanilla `/list` and online-name suggestions.
- Hidden advancement, dimension transition, death, and respawn remaining spectator; server console audit messages preserved.
- Restart after UUID removal, observer mode disabled, and mod uninstall: synthetic player logs in with original survival mode.
- Invalid configuration prevents mod/server loading and is not overwritten. Forge can return exit code zero despite a loading error; assert the error and absence of a started listener, not only exit status.

## Review fixes and bounded compatibility

- Runtime testing caught a Mixin 0.8.5 array-clone transformation failure; use `Arrays.copyOf` instead.
- Independent review caught stale saved modes after uninstall/reinstall, missing advancement/team-death announcements, restored mount passenger references, shared-packet mutation risk and a two-read volatile-snapshot race. These paths were corrected.
- Outgoing sanitation is at the `Connection.send` boundary, guarded by a server PLAY listener; login, status and authentication packets are untouched. Player-info updates/removals and nested bundles are copied only if filtering is needed.
- Installed TrueUUID 1.3.0 bytecode audit confirms verified UUID replacement occurs before player creation/placement, so early UUID checks see the final identity. Its delayed skin-refresh packets use the normal server play-listener send route. A separate synthetic refresh fixture exercises delayed remove/init and mixed visible/hidden bundles; it is not part of the production JAR.
- Inspected TFG inventory has no active Minecraft Discord bridge. A retained Discord integration config is not evidence of an active bridge. Operator/RCON automation intentionally sees authoritative players.

## Not represented as tested

- Full TFG client/modpack integration or live server activation.
- Every third-party scoreboard, map, quest, team, custom UI or custom payload. Those mods can expose real server state independently.
- Full legacy 1.6 custom ping request, basic GS4 response, all team death-visibility combinations, and restored mounted-login behavior have exact-hook/source review but are not all individually exercised by the protocol probe.
- Arbitrary deliberate commands/chat and world effects in non-observer mode are not anonymity guarantees.

See the integration probe instructions for reproduction. Use a separately reviewed maintenance plan for actual server deployment.
