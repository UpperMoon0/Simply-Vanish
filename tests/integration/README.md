# Isolated protocol probes

Never run these against a production server. The probe connects only to `127.0.0.1:25575`; Query probes use UDP `127.0.0.1:25576`. Use a throwaway Forge 47.4.13 world and synthetic users, accept Minecraft's EULA yourself, and explicitly approve offline authentication for that isolated fixture. Do not change an existing server's authentication.

Install Node dependencies with `npm ci` here. Start the installed dedicated server with the production reobfuscated mod JAR, `server-ip=127.0.0.1`, `server-port=25575`, `online-mode=false`, `enable-query=true`, `query.port=25576`, and no RCON. Use a flat world and `/gamerule spawnRadius 0` so the visible tracking assertion is non-vacuous.

Fixture config:

```json
{"hiddenUuids":["c6dfdb66-b0fa-3afa-9ecc-2de806cbaece"],"observerMode":true}
```

That UUID is derived from the synthetic offline name `VanishFixture`; it is not a real user's identity. Other synthetic users are `VisibleFixtureA` and `VisibleFixtureB`.

Run `node probe.cjs`. It checks initial/later viewers, a real visible-player tracking control, Tab, 30-second latency, join/leave, modern and legacy status, full Query and ordinary command suggestions. For the optional dimension/death cases, set `VANISH_TEST_COMMAND_SCENARIOS=1` and, after `VisibleFixtureB` joins, issue on the fixture console:

```text
advancement revoke VanishFixture only minecraft:story/enter_the_nether
advancement grant VanishFixture only minecraft:story/root
execute in minecraft:the_nether run tp VanishFixture 0 100 0
kill VanishFixture
list
```

Space these commands about two seconds apart. The probe automatically confirms teleports and requests respawn. Console `list` should show all three synthetic players while public probes show only two. The test report defaults to `/tmp/vanish-probe-report.json`; set `VANISH_TEST_REPORT` to choose another output.

For restoration, stop the fixture, remove the UUID or set `observerMode=false`, restart, and run `EXPECT_MODE=0 node mode-probe.cjs`. Repeat after removing the JAR from the fixture. The synthetic player must log in as survival, not remain spectator. Shut down and discard the fixture after testing.

## Delayed skin-refresh regression

`../../gradlew -p ../.. fixtureJar` builds a separate test-only mod under `build/fixture`, not `build/libs`. Install it only in the disposable fixture alongside the production JAR, and set `VANISH_TEST_REFRESH_FIXTURE=1` for the probe. After both synthetic users join, it sends delayed hidden remove/initialize packets plus a mixed visible/hidden bundle. The probe requires visible entries to survive while hidden entries remain absent. The fixture log must contain `VANISHGUARD_DELAYED_REFRESH_FIXTURE_SENT`. Never release or install that test JAR on a real server.
