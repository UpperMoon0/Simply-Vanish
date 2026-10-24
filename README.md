# Simply Vanish

Server-only, administrator-configured presence hiding for **Minecraft 1.20.1 / Forge 47.4.13**, version **0.1.0**. No client-side mod or custom network channel is required. This is not an authentication plugin or an anti-audit tool.

## Configuration

On first startup, the mod creates `config/simply-vanish.json` only if it is absent:

```json
{
  "hiddenUuids": [],
  "observerMode": true
}
```

An administrator adds canonical UUID strings to `hiddenUuids`. Do not put names, shortened UUIDs, or comments in the JSON. The UUID must be the one the server actually assigns after authentication; identity-remapping mods may change it. There are no built-in player identities or player-facing toggle commands.

**Changes take effect on the next planned server restart.** There is no automatic reload or partially applied state. Invalid JSON, malformed/duplicate UUIDs, unknown/duplicate keys, or unreadable configuration **prevent mod/server startup** with an error. They do not silently disable vanish. Back up and validate configuration before a maintenance restart. The pack manager must not overwrite this administrator-owned file.

## Behavior

Hidden players are excluded from entity tracking, other players' Tab/player-info packets (including initial login and latency updates), public join/leave announcements, modern status player count/sample, legacy ping, Query player count/names, player-issued vanilla `/list` (including `/list uuids`), and ordinary online-name suggestions. A hidden player can still receive their own player-info state.

With `observerMode: true` (default), hidden players use spectator mode while connected. Their original current/previous modes are preserved in player data; saved vanilla game-mode fields retain their original values, so a subsequent login after removing the UUID, disabling observer mode, or uninstalling the mod does not strand them in spectator. Mode changes away from spectator are refused while configured hidden. This prevents ordinary block breaking, combat, pickup, pressure plates and mob targeting; it is not a world-simulation pause. Chunk loading, server ticks and other indirect effects continue.

With `observerMode: false`, presence hiding remains enabled but gameplay interactions are allowed. Block changes, sounds, particles, combat, chat and other world effects may reveal activity. Use that setting only with this limitation understood.

## Deliberate limits

- Authentication, bans, whitelist, connection logs, server-console join/leave audit, real server slot accounting, saving and administrative APIs are untouched.
- Console/RCON `/list` and administrative selectors retain the real player set. This is intentionally not hidden from administrators.
- Commands/chat can reveal the sender. Do not use this as an anonymity or anti-detection system.
- Third-party Discord bridges, maps, team/scoreboard UIs, quest mods and custom player-list commands can read real server state independently. Compatibility must be separately verified. Do not assume a generic mod is covered because vanilla Tab is hidden.
- Existing scoreboard/team/history records are not erased. Other mods can send their own packets. An incompatible required mixin causes startup failure rather than silently dropping its hook.
- This mod changes no login/auth configuration and grants no operator permission.

## Build

Use Java 17:

```sh
./gradlew --no-daemon test build
```

Artifact: `build/libs/simply-vanish-1.20.1-0.1.0.jar` (reobfuscated for production Forge). Runtime dependencies: Minecraft 1.20.1 and Forge 47.4.13 only. Install only on the dedicated server, not clients. Never replace a running server's files or restart it without the administrator's maintenance approval.

See [verification](docs/verification.md) for measured results and remaining release gates. The source is MIT licensed; the Gradle wrapper retains its upstream license notices. No decompiled Minecraft sources are distributed here.
