# Munchaholic for Fabric (Java Edition)

This is the Fabric mod for Minecraft Java **26.3** (the default) and **26.2**. For what the mod does and how to install it, see the [root README](../README.md).

## Requirements

- JDK 21 or newer to run Gradle. The build itself compiles with a **Java 25 toolchain**, which Gradle downloads automatically through the foojay resolver. To use a JDK you already have, pass `-Porg.gradle.java.installations.paths=/path/to/jdk-25`.
- Everything else comes from the Gradle wrapper (Gradle 9.5.1, Fabric Loom 1.17, Loader 0.19.5, Fabric API 0.161.0).
- Mod Menu (optional, 21.0.0 for 26.3 / 20.0.2 for 26.2) to open the settings screen. The build only compiles against it (`clientCompileOnly`, from the TerraformersMC maven); it is not bundled and not on the dev/gametest runtime classpath. To try it in `runClient`, drop the matching `modmenu-*.jar` into `run/mods`.

## Build and test

```bash
./gradlew build                          # 26.3: compile + JUnit + server gametests
./gradlew build -Pmod_version=1.2.3      # set the version (default 0.0.0)
./gradlew clean build -Pmc=26.2          # build and test against 26.2 instead
./gradlew test                           # JUnit only (pure core logic + lang files)
./gradlew runGameTest                    # server gametests only (headless)
./gradlew runClient                      # dev client
```

`build/libs/` gets `munchaholic-<version>.jar`, the jar you ship, and `munchaholic-<version>-sources.jar`.

The client gametests open a real client and need a display. They are not part of `build`:

```bash
timeout 300 xvfb-run -a env LIBGL_ALWAYS_SOFTWARE=1 SDL_VIDEO_FORCE_EGL=1 ./gradlew runClientGameTest   # needs libegl1 libegl-mesa0
```

## Layout

| Path | What it holds |
|---|---|
| `src/main/java/dev/munchaholic/core/` | Pure roll logic: the attribute table with every step and cap (`Caps`, `AttributeSpec`), per-player steps (`PlayerStacks`, `Discoveries`), the Random-mode picker (`RollSelector`), the seed-based recipe mapping (`Recipes`) and number formatting (`Numbers`). It must not import Minecraft or Fabric classes, and `CorePurityTest` checks this. |
| `src/main/java/dev/munchaholic/` | Minecraft glue: the entrypoint (`Munchaholic`), the eat hook target (`BiteHandler`), the actionbar and sound (`Feedback`) and the shared text builders (`Texts`). |
| `src/main/java/dev/munchaholic/mode/` | The per-world settings: `MunchaholicMode` (SavedData), `ModeBootstrap` (new-world handoff and defaults) and the `PendingWorldMode` duck. |
| `src/main/java/dev/munchaholic/command/` | `MunchaholicCommand`, the `/munchaholic` command tree. |
| `src/main/java/dev/munchaholic/player/` | Per-player state: `MunchAttachments` (the two data attachments), `PlayerMunch` (steps → the `munchaholic:bites` modifiers, reset), `AttributeHolders`, `PlayerHooks` (join, death, respawn) and `RecipeSync`. |
| `src/main/java/dev/munchaholic/net/` | `RolledPayload` (`munchaholic:rolled`, the roll message for clients that have the mod) and `RecipesPayload` (`munchaholic:recipes`, the discovered recipes for tooltips). |
| `src/main/java/dev/munchaholic/mixin/` | Common mixins: the eat hooks (`FoodPropertiesMixin`, `CakeBlockMixin`), a duck on `LevelStorageSource.LevelStorageAccess` that carries the Create World choices to the integrated server, and a `MinecraftServer` accessor. |
| `src/client/` | The Create World buttons: `GameTabMixin` adds **Munchaholic Mode** and **Roll Mode** under Difficulty, `CreateWorldScreenMixin` holds their values and hands them to the new world. Recipe tooltips: `ClientRecipeBook` (payload receiver) and `RecipeTooltip`. Notification settings: `NotifyConfig` (loads/saves `config/munchaholic.json` via the pure `core/NotifySettings`), `NotifyClient` (payload receiver), `NotifySettingsScreen`, `ModMenuIntegration` (Mod Menu entrypoint only) and `NotifyCommand` (`/munchaholic-notify`). |
| `src/main/resources/assets/munchaholic/lang/` | `en_us.json` and `ru_ru.json` (same keys; see [Languages](#languages)). |
| `src/test/` | JUnit tests: the pure core (`CapsTest`, rolls, recipes, numbers, `CorePurityTest`) and `LangFileTest`. |
| `src/gametest/` | Server and client gametests. This is a separate test mod, `munchaholic-gametest`, and it is never packaged. Server (6 classes, 80 tests, run by `build`): `MunchEatingGameTests` (which consumptions roll: every vanilla food, cake slices, drinks, Creative/Spectator, mode OFF, fake players), `MunchRollGameTests` (modifiers from steps, stacking, the capped reroll, long random walks inside the caps, the growth and ledge vetoes), `MunchDeathGameTests` (keep-on-death, End exit, save/load), `MunchRecipesGameTests` (fixed effects per seed, discovery, capped recipes, tooltip sync), `MunchModeGameTests` (per-world settings, `mode.dat`, the `/munchaholic` tree and its permissions) and `MunchNotifyGameTests` (payload vs. overlay + sound path, message keys, argument order and colors). Client (3 tests, `runClientGameTest`): `MunchaholicClientGameTest` (Create World buttons and the handoff to the new world), `MunchaholicNotifyClientGameTest` (settings screen, `/munchaholic-notify`, the actionbar) and `MunchaholicTooltipClientGameTest` (recipe tooltips). |

## Behavior summary

- The settings are stored per world in `data/munchaholic/mode.dat`: `enabled`, `rollMode` (`random` or `recipes`) and `keepOnDeath`. The Create World → Game buttons start at ON and Random, and keep-on-death starts on (it has no button). Worlds created elsewhere (dedicated servers, worlds made without the mod) get OFF, Random and keep-on-death on, written at once. An existing file is never overwritten on load. There is no game rule.
- A roll happens when a Survival or Adventure `ServerPlayer` finishes a food (`FoodProperties.onConsume`) or eats a cake slice (`CakeBlock.eat`) while the mode is on. Fake players, other entities and the client side are filtered out. The food key is the item id; plain and candle cake both count as `minecraft:cake`. Potions, milk and the ominous bottle have no food component, so they never roll. A player with full hunger can't eat cake, so there is no bite.
- Random mode picks one of the 20 attributes uniformly and a 50/50 direction. If that step would leave the cap range, the attribute is dropped and a different one is rolled. Once all have been tried, it picks uniformly among the pairs that are still legal. Recipes mode maps (world seed, food id) to one attribute and direction by rendezvous hashing (`core/Recipes`); reordering the table never changes a world's recipes. A capped recipe changes nothing, but the bite counts, the food is discovered and the "at the limit" message is shown (its change and limit value in gray).
- Each player stores integer steps per attribute plus a bite counter in the `munchaholic:stacks` attachment (persistent, not copied on death), and discovered foods in `munchaholic:discoveries` (persistent and copied on death). Each changed attribute gets one permanent modifier, `munchaholic:bites`, with amount steps × step (`ADD_MULTIPLIED_BASE` for percentages, `ADD_VALUE` for points). It is recomputed from the steps after every roll and on join, respawn and reset, so a modifier removed with `/attribute` comes back on the next join.
- Caps live only in `core/Caps` and apply to the base value plus our modifier; gear, effects and other mods' modifiers are ignored. Every cap lies inside the vanilla attribute range, so vanilla never clamps our part. `CapsTest` freezes the reachable ranges. Player-facing table: [root README](../README.md#attributes-and-safety-caps).
- Two cross-attribute vetoes are treated exactly like a cap (Random rerolls, Recipes shows "at the limit"): `core/Mobility` keeps every player able to climb a 1-block ledge (a simulated standing jump of at least 1.02 blocks, or Step Height of at least 1; at the vanilla bases jump goes down to 90% and gravity up to 130%), and `BiteHandler.growthVeto` refuses a Scale increase whose bigger hitbox wouldn't fit where the player stands. Size isn't re-checked afterwards (for example on respawn).
- Keep-on-death ON: on respawn the steps are copied to the new player, the modifiers reapplied and health set to the new maximum. OFF: the changes and the bite count are lost, and the player gets one chat line if they had any. Discoveries always survive. Leaving the End keeps everything. Turning the mode OFF freezes changes; only `/munchaholic reset` removes them.
- `/munchaholic` (op level 2 = `LEVEL_GAMEMASTERS`): reading is open to everyone (bare, `status`, bare `mode`, bare `keep-on-death`, `stats` for yourself). `on|off`, `mode random|recipes`, `keep-on-death on|off`, `stats <player>`, `reset <targets>` and `reset <targets> recipes` need op level 2 and are broadcast to ops. Results: status and `on|off` 1 = ON, 0 = OFF; `mode` returns 0 = Random, 1 = Recipes; `keep-on-death` 1/0; `stats` the number of changed attributes; `reset` the number of targets.
- Recipe tooltips: the server sends `RecipesPayload(active, entries)` with **only the discovered** foods, on join, after a new discovery, after respawn and after `reset … recipes`, and to everyone on `on`, `off` and `mode` changes. `active` = mode on and Recipes. The client shows `Munchaholic: <attribute> ▲/▼` on discovered foods and `Munchaholic: ??? (eat it to find out)` on the others, only while active. The client never gets the world seed.

### Languages

- English (`en_us`) and Russian (`ru_ru`), in `src/main/resources/assets/munchaholic/lang/`. Every player-facing string (Create World buttons and tooltips, `/munchaholic` and `/munchaholic-notify` feedback, the settings screen, the roll actionbar, the recipe tooltip, the Mod Menu summary) is a translation key. Attribute names come from vanilla's own `attribute.name.*` keys, and ON/OFF on buttons and Done from the game's translation.
- Server-side strings keep an English fallback (`translatableWithFallback`) for players on vanilla clients, who have no mod lang files.
- Numbers use ASCII `+`/`-` and a `.` decimal point in every language. Percentages are whole numbers, and points use up to 2 decimals. The units (`%s%%`, `%s`) are translation keys.
- `LangFileTest` checks that `ru_ru.json` has exactly the keys of `en_us.json`, the same `%s`/`%1$s` placeholders per key, the same `%%` count and no empty values, and that every fallback in the code equals the `en_us` text (including the roll mode names that `Texts.englishRollMode` supplies for the run-time `munchaholic.rollMode.*` keys). Add a key to both files.

### Notification settings

- Each roll shows an actionbar message and plays a quiet amethyst chime (higher pitch for a buff, lower for a debuff, a middle pitch when capped or when nothing can change). Each player can turn either one off, or both. The settings are client-side and per player, stored in `config/munchaholic.json`: `{"notifySound": true, "notifyMessage": true}`. A missing or broken file means both are on; a broken file is left alone until the next change replaces it.
- With [Mod Menu](https://modrinth.com/mod/modmenu) installed (optional, `suggests`), Mods → Munchaholic → the config button opens the settings screen (**Roll sound** / **Roll message**, saved on every click).
- Without Mod Menu, use the client command `/munchaholic-notify status`, `/munchaholic-notify sound|message` (switches it) or `/munchaholic-notify sound|message on|off`. The root is `munchaholic-notify`, not `munchaholic notify`, so it cannot clash with the server's `/munchaholic` command.
- This needs the mod on both client and server: when the client has the mod, the server sends only the `munchaholic:rolled` payload and the client shows/plays according to its settings. Players who join a server-only install (vanilla client) always get the default actionbar + sound, and no recipe tooltips.

## Known quirks

These are vanilla behaviors and are not patched:

- A large Scale doesn't fit through doors or 1-block tunnels, and a head inside a block takes suffocation damage. A small Scale fits through gaps a normal player can't.
- A roll never grows a player beyond the space they stand in, but size isn't re-checked later: a large player who respawns at a bed in a small room (keep-on-death on) can take suffocation damage. Fix: a roomier bed, a shrinking food, or `/munchaholic reset <player>`.
- Only items with a food component and cake-block slices roll. Blocks that feed the player some other way (some modded pies) don't.
- Step Height above 1 walks up full blocks, and above 1.5 over fences and walls.
- High Jump Strength can cause fall damage from an ordinary jump. Low Gravity makes jumps float.
- Low Block Interaction Range makes breaking and placing blocks awkward. High Entity Interaction Range hits mobs from far away.
- A lower Max Health removes hearts at once (Munchaholic clamps the current health immediately). A higher one doesn't heal.
- Attack Damage and Knockback Resistance aren't synced to the client, so client-side attribute displays show the vanilla values.
- Mining Efficiency only applies with a tool that suits the block. Luck only affects some loot tables.
- Permanent modifiers are saved with the player, so they stay after the mod is removed. Reset players first.
