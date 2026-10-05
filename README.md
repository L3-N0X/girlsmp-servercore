# Server Core

Server-side Fabric mod (Kotlin) with custom content for the server: tab list, bossbar and more to come.
Players only need a **vanilla Minecraft 26.3 client** to join; nothing has to be installed on the client.

| | Version |
|---|---|
| Minecraft | 26.3 |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.161.0+26.3 |
| Fabric Language Kotlin | 1.14.1+kotlin.2.4.20 |
| adventure-platform-fabric | 7.2.0 (Adventure 5.2.0, bundled) |
| Kotlin | 2.4.20 |
| Java | 25 |
| Gradle | 9.5.0 (wrapper) |

All versions are in `gradle.properties`. Check https://fabricmc.net/develop for updates.

## Prerequisites

- **JDK 25.** Gradle runs on Java 25 (`gradle/gradle-daemon-jvm.properties`), so a different
  `JAVA_HOME` doesn't matter, but a JDK 25 has to be installed somewhere Gradle can find it.
- A **Minecraft Java Edition 26.3** client (the normal launcher works, no mods needed).

## Quick start: run the dev server and join with your client

1. **Start the dev server once.** It creates `run/` and then stops because the EULA isn't accepted yet:

   ```bash
   ./gradlew runServer
   ```

2. **Accept the EULA** (https://aka.ms/MinecraftEULA):

   ```bash
   echo "eula=true" > run/eula.txt
   ```

3. **Optional: adjust `run/server.properties`** (created on the first run). Useful settings for testing:

   ```properties
   online-mode=true        # keep true to join with your normal Microsoft account
   gamemode=creative
   difficulty=peaceful
   spawn-protection=0
   view-distance=8
   ```

   Set `online-mode=false` only if you want to join with several offline test accounts
   (for example from a second, modded dev client).

4. **Start the server again:**

   ```bash
   ./gradlew runServer
   ```

   Wait for `Done (...)! For help, type "help"`. Your mod logs `Server Core initialized` at startup and
   `Enabled modules: tablist, bossbar, playerstats, scoreboard, resourcepack, vanish` once the server is running.

5. **Give yourself op.** Type this in the server console (the terminal running Gradle):

   ```
   op <YourMinecraftName>
   ```

6. **Join with your client.** Open the Minecraft launcher, start **26.3** (vanilla), then go to
   *Multiplayer → Direct Connection* and enter `localhost`. Port 25565 is the default.

7. **Stop the server** by typing `stop` in the console. Avoid Ctrl+C so the world gets saved cleanly.

To reset the test world, delete `run/world/`. Everything in `run/` is git-ignored.

### Iterating

- **From the terminal:** stop the server, change code, then run `./gradlew runServer` again. Your
  client can stay open; just reconnect.
- **From IntelliJ IDEA:** open the folder as a Gradle project. Loom creates a **Minecraft Server** run
  configuration. Start it with **Debug** to get breakpoints. Method-body changes can be hot-swapped
  without a restart via *Run → Debugging Actions → Reload Changed Classes*. Structural changes such
  as new methods or classes still require a restart.

## Building and deploying to the real server

```bash
./gradlew build
```

The mod jar is `build/libs/servercore-<version>.jar`. Ignore the `-sources` jar.

On the real Fabric 26.3 server, put these files into `mods/`:

- `servercore-<version>.jar`
- [Fabric API](https://modrinth.com/mod/fabric-api) for 26.3
- [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)

Adventure doesn't need to be installed: it's bundled inside the mod jar (jar-in-jar).

The Fabric server launcher is available at https://fabricmc.net/use/server/.

## Project structure

```
src/main/kotlin/de/lenox/servercore/
├── ServerCore.kt              # ModInitializer entrypoint, registers modules
├── commands/
│   ├── Commands.kt            # Brigadier command registration
│   ├── StatsCommand.kt        # /stats [player], /stats top, /stats toggle, /stats reload
│   ├── ResourcePackCommand.kt # /resourcepack [reload]
│   ├── VanishCommand.kt       # /vanish
│   └── InvseeCommand.kt       # /invsee <player>
└── core/
    ├── ServerModule.kt        # Module interface: lifecycle, tick, join/leave hooks
    ├── ModuleManager.kt       # Hooks Fabric API events and dispatches them to modules
    ├── ServerThread.kt        # onServerThread { }: hop back to the server thread from async code
    ├── storage/
    │   ├── Storage.kt         # Data folder, shared JSON format, background I/O scope
    │   └── JsonFileStore.kt   # Async folder of JSON files, one file per key
    ├── utils/components/      # Text: Cmp (builder + gradients), Theme (colours), Color (shade/lighter/...)
    ├── resources/             # Font keys, Sprite (all pack images), ResourcePackModule (sends the pack)
    ├── scoreboard/            # Packet-only sidebar: Sidebar, ScoreboardModule, ScoreboardProvider
    ├── stats/                 # Playtime, joins, deaths, totem pops; vanilla objectives, StatsSidebar
    ├── tablist/               # Packet tab list header/footer/scores: TabListModule, TabListScores, ServerTabList
    ├── invsee/                # InvseeMenu: the chest GUI of /invsee
    ├── vanish/                # VanishModule: hidden operators (the vanilla hooks are mixins)
    └── bossbar/BossBarModule.kt   # (stub) per-player bossbars
src/main/java/de/lenox/servercore/mixin/   # Mixins (Java): ServerScoreboardMixin, LivingEntityMixin (totem pops), vanish hooks
src/main/resources/fabric.mod.json, servercore.mixins.json
resourcepack/                  # Server resource pack sources (fonts, sprites), build.sh, split_sprite.py
.github/workflows/resourcepack.yml   # Builds + releases the pack on changes to resourcepack/
```

To add a feature, create an `object` implementing `ServerModule` and register it in
`ServerCore.onInitialize()` with `ModuleManager.register(...)`.

## Text and colours

Text uses [Adventure](https://docs.advntr.dev/), the same API as Paper, with the Eventrox helpers
`Cmp` and `Theme` (package `core.utils.components`):

```kotlin
player.sendMessage(Cmp(Cmp("» ", Theme.SUBTEXT_3), Cmp("Welcome!", Theme.LIGHT_PURPLE, Theme.LIGHT_BLUE)))
source.sendSuccess(Cmp("Done", Theme.LIGHT_GREEN), false)       // command feedback
Cmp.gradient("Rainbow", Theme.RED, Theme.GOLD, Theme.MINT)       // any number of colour stops
"Hello".gradient(Theme.PINK, Theme.PURPLE)                       // same, as an extension
Cmp.mini("<gradient:#B6ABFB:#7496E3>From config</gradient>")     // MiniMessage, for trusted strings only
```

- `ServerPlayer` and `MinecraftServer` are Adventure `Audience`s (`sendMessage`, `sendActionBar`, `showTitle`,
  `showBossBar`, `playSound`, ...) and `CommandSourceStack` has `sendSuccess`/`sendFailure` overloads for
  Adventure components. adventure-platform-fabric adds these through interface injection, so they compile like
  normal methods.
- Use `Theme` colours instead of `ChatFormatting`. `Cmp` gradients are built per character without
  MiniMessage, so player names in them can't inject tags.
- Vanilla APIs that only take `net.minecraft.network.chat.Component` get one through `component.toNative(server)`.
- Custom fonts from the [resource pack](#resource-pack) are in `Font` (same names as in Eventrox, namespace
  `girlsmp`): `Cmp("playtime", Theme.SUBTEXT_2, Font.CAPS)`, `Cmp("12", Theme.GOLD, Font.MONO)`,
  `Cmp("Title", Theme.PINK, Theme.PURPLE, Font.CAPS)`. `Cmp.space(px)` moves text by pixels (negative = left).

| Font | Looks like |
|---|---|
| `Font.CAPS` / `CAPS_CENTER` | Pixel caps: lowercase letters become small caps. `"12".capsWithLargeDigits()` for full-height digits |
| `Font.MONO` / `BIG_MONO` | Monospaced (6 px per character), for numbers and columns |
| `Font.BIG` | Bigger pixel font |
| `Font.DEFAULT_XL` | Vanilla font, bigger |
| `Font.SHIFT_UP` | Small caps shifted 9 px up (a line above the text) |
| `Font.SPACE` / `Cmp.space(...)` | Pixel spaces from -8192 to 8192 |
| `Font.PIXELIZED` | Pixel fractions for bars (`pixel.*` translation keys) |
| `Font.SPRITE` | Images, use them through `Sprite` (see [Sprites](#sprites)) |

## Sidebar

`ScoreboardModule` shows a sidebar that only exists as packets (like FastBoard on Paper): it never
touches the server's scoreboard, so every player can see different content, and vanilla limits and
`/scoreboard` don't apply. It uses the modern scoreboard features (line text as score display name, hidden
numbers, right-aligned text per line), sends only the lines that changed and supports up to 15 lines.

Content comes from the installed provider, re-rendered every tick, same as Eventrox:

```kotlin
ScoreboardModule.provider = object : ScoreboardProvider {
	override fun getTitle(player: ServerPlayer) = Cmp("My Game", Theme.GOLD, Theme.OCKER)
	override fun getLines(player: ServerPlayer) = listOf(Cmp("Kills", Theme.SUBTEXT_1))
	override fun getScores(player: ServerPlayer) = listOf(Cmp("3", Theme.LIGHT_RED)) // right-aligned, optional
}
ScoreboardModule.provider = null // hides the sidebar
```

The default provider is `StatsSidebar` (playtime, joins, online players). Players turn it off and on for
themselves with `/stats toggle` (saved in `servercore/scoreboard/settings.json`). If vanilla puts an objective in the
sidebar slot (`/scoreboard objectives setdisplay sidebar ...`), `ServerScoreboardMixin` immediately sends our
sidebar again. Team-coloured sidebar slots (`sidebar.team.<color>`) still take priority on the client.

## Tab list

`TabListModule` renders the tab list every tick and only re-sends what changed:

- **Header and footer** (per viewer) with `ClientboundTabListPacket`.
- **Scores**, the text right of each name (the same for every viewer), as a packet-only objective in the list
  slot (`TabListScores`, built like the sidebar): every score has its own fixed text, so it can be any component,
  icons included. While no player has a score, the list slot is left to vanilla (`/scoreboard objectives
  setdisplay list ...`); otherwise `ServerScoreboardMixin` puts ours back whenever vanilla changes that slot.

The default provider is `ServerTabList`: the server banner above the player list and a skull with the death
count next to every name.

```kotlin
TabListModule.provider = object : TabListProvider {
	override fun getHeader(player: ServerPlayer) = Cmp(Sprite.BANNER.toBlock(), Cmp.newline(), Cmp("Hi", Theme.PINK))
	override fun getFooter(player: ServerPlayer) = Cmp("developed by lenox", Theme.SUBTEXT_2, Font.CAPS)
	override fun getScore(player: ServerPlayer) = Cmp("${player.connection.latency()}ms", Theme.SUBTEXT_2, Font.MONO)
}
TabListModule.provider = null // clears header, footer and scores
```

## Sprites

Every image of the resource pack is an entry of the `Sprite` enum (like Eventrox's `SpriteComponent`): glyphs
of the `girlsmp:sprite` font (`resourcepack/assets/girlsmp/font/sprite.json`), one set per size.

```kotlin
Sprite.SKULL.toComponent(Sprite.Size.TEXT)     // inline, as high as a letter
Sprite.BANNER.toComponent(Sprite.Size.SMALL)   // inline, reaching up into the line above
Sprite.BANNER.toBlock(Sprite.Size.LARGE)       // with empty lines above, for multi-line text like the tab list
```

| Size | Height | Lines it covers |
|---|---|---|
| `TEXT` | 8 px | 1 (sits on the baseline like a letter) |
| `SMALL` | 16 px | 2 |
| `BASE` | 32 px | 4 |
| `LARGE` | 48 px | 6 |

Glyphs have `ascent` = `height`, so they draw upwards from the baseline; `toBlock` puts `lines - 1` empty
lines above the sprite so it doesn't cover the text above. `TEXT` glyphs use `ascent` 7 like the default font,
so their bottom row hangs 1 px below the baseline (leave it empty to end on the baseline).

**Icons:** `SKULL` (`textures/font/sprite/skull.png`, 8x8, `U+E00C`) is the death counter in the tab list.

**The banner** is 256x64 px, cut into 4 tiles of 64x64 (`textures/font/sprite/banner_0..3.png`, code points
`U+E000..E00B`: 4 per size). Tiles are drawn side by side with a -1 px space (`U+F801`) between them, since every
bitmap glyph advances 1 px more than it is wide. The current artwork is a placeholder; to replace it, save a
256x64 PNG and run

```bash
resourcepack/split_sprite.py my_banner.png banner 4
```

**Adding a sprite:** put the PNG(s) into `textures/font/sprite/` (wide images through `split_sprite.py`), add a
`bitmap` provider per size to `sprite.json` with new code points, and an enum entry in `Sprite.kt` with the same
code points (`tiles(first, count)` for tiled images).

## Resource pack

`resourcepack/` is the server resource pack (the Eventrox fonts, renamed to the `girlsmp`
namespace, the space and pixelized fonts and the [sprites](#sprites)). Every player gets it automatically when joining.

**Publishing:** push changes under `resourcepack/` to `main`. The *Resource pack* workflow builds the zip
(`resourcepack/build.sh`), bumps the patch version (or uses the version you enter when starting it by hand under
*Actions*) and creates the release `pack-v<version>` with the SHA-1 in its notes. The zip is also committed into
the release tag, so jsDelivr serves it from its worldwide CDN:
`https://cdn.jsdelivr.net/gh/L3-N0X/girlsmp-servercore@pack-v<version>/girlsmp-resourcepack.zip`.

**On the server:** `ResourcePackModule` looks up the newest `pack-v*` release through the GitHub API on startup
and sends it in the configuration phase, like vanilla's `resource-pack` setting: the pack downloads in the
loading screen before the player enters the world, and declining a required pack disconnects. After publishing,
run `/resourcepack reload` (op) to pick up the new version and push it to everyone online; `/resourcepack`
shows the current version. Settings are in `servercore/config/resourcepack.json` (repository, jsDelivr or GitHub
download, required). If `server.properties` sets `resource-pack`, that one is used instead.

**Testing locally:** `resourcepack/build.sh` writes `build/resourcepack/girlsmp-resourcepack.zip` (reproducible:
the same files always give the same SHA-1). Put it into `.minecraft/resourcepacks/` to try changes before
pushing.

## Data files

All persistent data lives in `servercore/` next to `server.properties` (`run/servercore/` in dev),
as pretty printed JSON that can be edited by hand.

`JsonFileStore` handles the files: all reads and writes run in the background, in call order, and
every write goes through a temp file + atomic move, so a crash can't leave a half written file.
A file that can't be parsed is renamed to `<name>.json.broken-<timestamp>` and logged instead of
being overwritten. Unknown fields are ignored and missing ones use their defaults.

### Player stats

`servercore/playerstats/<uuid>.json`:

```json
{
  "name": "Notch",
  "playtimeSeconds": 7260,
  "joins": 12,
  "deaths": 3,
  "totemPops": 1,
  "firstJoin": "2026-10-02T10:15:00Z",
  "lastSeen": "2026-10-02T12:16:00Z"
}
```

- Playtime is wall-clock time while online. Joins are written to disk right on join; playtime is
  saved on leave, every 5 minutes and on shutdown.
- Deaths and totem pops (a totem of undying saving the player) are counted from the moment this was
  added, not taken from the vanilla statistics, and saved right away.
- Offline players' files can be edited at any time. **Online players' files are overwritten** by
  the in-memory copy, so after editing one run `/stats reload` (op): it re-reads all files and
  drops whatever wasn't saved yet.
- The stats are shown in the sidebar (see [Sidebar](#sidebar)), deaths also in the [tab list](#tab-list),
  and they are mirrored to the vanilla scoreboard objectives `playtime` (minutes), `joins`, `deaths` and
  `totem_pops` for datapacks and command blocks. The JSON
  files are the source of truth: scores are rewritten from them on startup, on `/stats reload` and while
  playing.

| Command | Permission | |
|---|---|---|
| `/stats [player]` | everyone | Playtime, joins, deaths, totem pops, first join, last seen (offline players too) |
| `/stats top <playtime\|joins\|deaths\|totems>` | everyone | Top 10 |
| `/stats toggle` | everyone | Hide or show your sidebar |
| `/stats reload` | op (level 2) | Re-read the JSON files after editing them by hand |

### Vanish

`/vanish` (op, level 2) toggles vanish for yourself. A vanished player is invisible to **every other player**, in
every game mode, and stays vanished across rejoins and restarts (`servercore/vanish/vanished.json`, a list of UUIDs):

- No entity, no tab list entry, not in the server list count/sample, `/list` or the sidebar's online count, not
  counted for sleeping, ignored by mobs.
- No join, leave, death or advancement messages. Toggling while online sends a fake "left the game" /
  "joined the game", so it looks like a normal leave/join.
- Chatting (and `/msg`, `/me`, `/say`) works as usual. Others receive it as unsigned chat, because clients
  disconnect on signed chat from a player that isn't in their tab list.
- Non-ops can't find vanished players by name in commands (`/msg`, suggestions, `/stats` shows them offline).
  Ops, the console, command blocks and functions still can.
- Not hidden: what you do to the world (breaking/placing blocks, opening chests, their sounds) and your stats
  still being recorded (joins, playtime). Spectator mode avoids the former.

| Command | Permission | |
|---|---|---|
| `/vanish` | op (level 2) | Vanish, or become visible again |

### Invsee

`/invsee <player>` (op, level 2) opens the inventory of an **online** player as a 6 row chest GUI, laid out like the
vanilla inventory screen:

```
 ▪  head  chest  legs  feet  ▪  offhand  body  saddle
    main inventory (3 rows)
 ▪  ▪  ▪  ▪  ▪  ▪  ▪  ▪  ▪
    hotbar
```

- It's live and editable: both sides see changes right away. Armor, offhand, body armor and saddle slots take any
  item, like `/item replace`. Shift click moves items between the two inventories, into the target's main inventory
  or hotbar only.
- `▪` fillers can't be taken or filled. The GUI closes if the player leaves, dies or changes dimension.
- Offline players aren't supported, their data is only read from disk on join.

| Command | Permission | |
|---|---|---|
| `/invsee <player>` | op (level 2) | Open a player's inventory |

### Notes

- **Server-only build:** `loom { serverOnlyMinecraftJar() }` compiles against the dedicated server
  jar only, so referencing client-only classes fails at compile time. Since Minecraft 26.x is
  unobfuscated, the code uses Mojang's official names (`ServerPlayer`, `MinecraftServer`, ...).
- **Tab list / bossbar from the server:** vanilla already provides everything needed:
  `ClientboundTabListPacket` (header/footer, used by `TabListModule`), `ClientboundPlayerInfoUpdatePacket`
  (display names) and `ServerBossEvent` (bossbars). Text is built with Adventure (see [Text and colours](#text-and-colours)).
- **Kotlin libraries:** Fabric Language Kotlin ships the Kotlin stdlib, kotlinx.serialization and
  kotlinx.coroutines, so they don't need to be bundled. The serialization compiler plugin is
  already enabled for config classes.
- **Renaming the mod:** change `mod_id` in `gradle.properties`, `rootProject.name` in
  `settings.gradle.kts`, `MOD_ID` in `ServerCore.kt`, and the package / entrypoint in `fabric.mod.json`.
