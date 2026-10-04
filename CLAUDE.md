# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Server-side-only Fabric mod (Kotlin, package `de.lenox.servercore`) for the GirlSMP server, Minecraft 26.3, Java 25.
Players join with a **vanilla client**, so everything visible (sidebar, tab list, fonts, icons) is done with
vanilla packets plus a server resource pack. `README.md` is the detailed user-facing documentation (features,
commands, data file formats, resource pack pipeline); keep it in sync when behaviour changes.

Much of the text/UI layer (`Cmp`, `Theme`, `Font`, sprites, sidebar provider API) is ported from **Eventrox**, a
Paper plugin at `../eventrox` (its own `CLAUDE.md` describes it). Its resource pack sources are in
`../eventrox-resources`. Use them as reference, but this repo is Fabric: no Bukkit/Paper API.

## Commands

```bash
./gradlew build          # jar in build/libs/servercore-<version>.jar (ignore -sources)
./gradlew compileKotlin  # fastest compile check (Java mixins: compileJava)
./gradlew runServer      # dev server in ./run (needs run/eula.txt with eula=true; join at localhost)
resourcepack/build.sh    # builds build/resourcepack/girlsmp-resourcepack.zip (reproducible SHA-1)
resourcepack/split_sprite.py <image.png> <name> <tiles>  # cut a wide image into sprite tiles
```

There is no test source set and no linter configured. Code uses tab indentation.

## Architecture

- **Server-only compile:** `loom { serverOnlyMinecraftJar() }`, so client classes don't exist. Minecraft 26.x is
  unobfuscated: use Mojang names (`ServerPlayer`, `MinecraftServer`, `ClientboundTabListPacket`, ...).
- **Modules:** every feature is a Kotlin `object` implementing `core/ServerModule` (lifecycle, tick, join/leave,
  death hooks). `ServerCore.onInitialize()` registers them with `ModuleManager.register(...)`, which subscribes to
  the Fabric API events once and fans them out, always on the server thread. Commands are Brigadier, registered in
  `commands/Commands.kt`.
- **Providers:** `ScoreboardModule.provider` (`ScoreboardProvider`) and `TabListModule.provider` (`TabListProvider`)
  are re-rendered every tick and only changed parts are sent. Defaults are `StatsSidebar` and `ServerTabList`; set
  to `null` to hide. Both the sidebar and tab-list scores are packet-only objectives that never touch the real
  server scoreboard; `ServerScoreboardMixin` re-sends them when vanilla takes over their display slot.
- **Mixins (Java, `src/main/java/.../mixin`):** must be listed in `src/main/resources/servercore.mixins.json`
  (`defaultRequire: 1`, so a missed injection target fails at startup). Mixins stay thin and call into Kotlin
  objects via `@JvmStatic` functions (e.g. `VanishModule.isHiddenFrom`). Naming: injected/handler methods are
  prefixed `servercore$`; duck interfaces implemented by mixins live in Kotlin and use `servercore_` method names
  (e.g. `EntityTracking`, cast `chunkMap as EntityTracking`). Vanish is mostly implemented this way across many
  mixins.
- **Persistence:** `core/storage`. `Storage.root` = `<server dir>/servercore/` (`run/servercore/` in dev).
  `JsonFileStore(folder, serializer)` = one pretty JSON file per key, all I/O off-thread on a single-parallelism
  dispatcher (ordered), atomic temp-file writes, unparsable files renamed to `.broken-<timestamp>`. Data classes
  are `@Serializable` with defaults (unknown keys ignored, hand edits tolerated). `Storage.awaitPendingWrites()`
  runs on `SERVER_STOPPED`. Never block the server thread on disk I/O (exception: tiny files that must be loaded
  before players join, like vanish); return to the server thread with `server.onServerThread { }` from coroutines.
- **Text:** Adventure (bundled jar-in-jar via adventure-platform-fabric). `ServerPlayer`/`MinecraftServer` are
  `Audience`s and `CommandSourceStack` has Adventure `sendSuccess`/`sendFailure` through interface injection. Build
  text with `Cmp(...)` and `Theme` colours (not `ChatFormatting`); `Cmp.mini` (MiniMessage) only for trusted
  strings. Convert for vanilla-only APIs with `component.toNative(server)`.
- **Resource pack:** sources in `resourcepack/` (namespace `girlsmp`, plus `space` and `pixelized`). Fonts map to
  `core/resources/Font.kt`; every image is a `Sprite` enum entry whose code points must match
  `resourcepack/assets/girlsmp/font/sprite.json` (one bitmap provider per size). Pushing changes under
  `resourcepack/` to `main` triggers `.github/workflows/resourcepack.yml`, which releases `pack-v<version>`;
  `ResourcePackModule` fetches the newest release via the GitHub API at startup (or on `/resourcepack reload`)
  and sends it during the configuration phase.

## Versions

All dependency versions are in `gradle.properties`. Renaming the mod touches `mod_id` there, `rootProject.name`
in `settings.gradle.kts`, `MOD_ID` in `ServerCore.kt`, and `fabric.mod.json`.
