# Munchaholic — Branding

> This file is the branding source for Munchaholic. The logo, icon, palette and text colors should match it; change them here first. The player-facing strings live in `en_us.json` (the tables below mirror it), so change the lang file, `ru_ru.json` and this file together.

**Tagline:** *Take a bite. Roll a stat. Never stop.*
Alt: *Take a bite. Change a stat. Never stop.* / *One more carrot can't hurt. Probably.*

## CurseForge description

See [curseforge_description.md](curseforge_description.md) (the text pasted into CurseForge).

## Features
- 🍖 **Every bite counts:** finishing any food (or a slice of cake) changes one attribute, up or down, for good.
- 🎲 **Buff or debuff:** each roll can go either way. Grow, shrink, float, sink, sprint or crawl. The chaos is the point.
- 📈 **It stacks:** changes build on each other bite after bite. Every attribute has a safety cap, so you can't shrink to nothing, lose your last heart or float away for good.
- 📖 **Two Roll Modes:** *Random* (any food, any stat) or *Recipes* (each food always changes the same stat in this world, so you can learn which foods to eat).
- ⚙️ **Toggle anywhere:** an ON/OFF button at world creation, plus an operator command (`/munchaholic`) for existing worlds and servers.
- 💬 **Clear feedback:** an actionbar message shows what you ate, what changed, and where the stat is now.
- 🔕 **Your call on noise:** turn the roll sound, the actionbar message, or both off (Mod Menu or `/munchaholic-notify`).
- 🧱 **Java + Fabric:** a Fabric mod for Java 26.2–26.3.

## Logo
`docs/branding/curseforge_logo.png` (400×400 RGBA) is the same layout as the other "-aholic" logos. The art is drawn on a 40×40 grid of 10 px cells. The frame is 4 px bands: `#140822` edge, `#783CBE` frame, `#3E1C60` inner band, and 16 px gold corner squares with an 8 px `#FFF4AA` centre. The background is `#26103C`. There is no text.

- **Central motif:** a roast drumstick running diagonally from the lower left to the upper right, with a big round bite taken out of the meaty end. The bite edge is pale flesh (cream, then pink). The crust is shaded in 4 diagonal tone bands, lit from the top left, with a glaze highlight streak and a few crispy-skin pixels. The bone is a 3-wide cream staircase that ends in two round knuckles. Everything has the `#180A28` 1-cell outline.
- **Kept from the series:** the three lilac/white sparkles (top left and top right), the green `+` at the bottom centre, and the gold chevrons at the bottom right. For Munchaholic the chevrons are an **up/down pair** (⌃ above, ⌄ below) to say "buff or debuff". They use the same placement, size and gold colors as Enchantaholic's stack.
- **Mod icon:** `fabric/src/main/resources/assets/munchaholic/icon.png` (128×128 RGBA) is built the same way as Enchantaholic's. Crop art cells 4–35 (32×32) from the logo, make the background transparent, drop the frame, and scale ×4 with nearest-neighbor.
- It reads at 128 px and 64 px: the bite, the bone and the up/down chevrons all stay distinct.

## Color palette
| Role | Hex |
|---|---|
| Background, deep purple | `#26103C` |
| Background, darkest / border | `#140822` |
| Inner frame band | `#3E1C60` |
| Ink / outline | `#180A28` |
| Frame purple | `#783CBE` |
| Sparkle lilac | `#D696FF` |
| Sparkle core | `#FFFFFF` |
| Level gold (chevrons, corners) | `#FFD640` |
| Level gold shade | `#B07010` |
| Corner gold light | `#FFF4AA` |
| Bonus green (+) | `#80FF40` |
| Crust glaze | `#FFD8A0` |
| Crust light | `#EE9A52` |
| Crust mid | `#C8682E` |
| Crust dark | `#8E3E22` |
| Crust shadow | `#5A2226` |
| Bitten flesh | `#FFE4CC` / `#F29C84` |
| Bone | `#FFFFF2` / `#EEE2C4` / `#C0AC8C` / `#7E6878` |

In-game text (`Feedback.java`, `Texts.java`, `RecipeTooltip.java`; colors are set in code, never in the lang files):

- **Roll message:** the whole line is `§d` (light purple), including ✦, → and the literal text. The food name is `§6` (gold), the attribute name `§f` (white), the change `§a` (green) for a buff and `§c` (red) for a debuff, and `(now …)` `§7` (gray).
- **"At the limit" message:** the same frame, but the change and the limit value are both gray. The "nothing left to change" message is light purple with a gold food name.
- **Recipe tooltip:** the line is gray (`Munchaholic:`), the attribute name white, and the ▲/▼ green for a buff or red for a debuff. The undiscovered line is `§8` (dark gray).
- **`/munchaholic stats`:** the attribute white, the change green/red, `(now …)` gray; the header is unstyled.
- **Everything else** (command feedback, the reset and lost-on-death chat lines) uses the default color. Mode names (`Random`, `Recipes`) are `§6` (gold).

## Player-facing strings (en_us)

The full list (46 keys) is `fabric/src/main/resources/assets/munchaholic/lang/en_us.json`; `LangFileTest` keeps `ru_ru.json` and the code fallbacks in sync with it. The tables below are a copy of it, grouped by where the text shows up. Attribute names come from vanilla's `attribute.name.*` keys, and ON/OFF on buttons and in `/munchaholic-notify` from vanilla's `options.on` / `options.off` (`CommonComponents.optionStatus`).

### Mod Menu

| Key | Text |
|---|---|
| `modmenu.summaryTranslation.munchaholic` | Take a bite. Roll a stat. Never stop. |
| `modmenu.descriptionTranslation.munchaholic` | Take a bite. Roll a stat. Never stop. Every time you finish eating, one random attribute (size, jump, speed, max health, gravity, reach…) is permanently buffed or debuffed, and the changes stack. |

### Create World screen

| Key | Text |
|---|---|
| `munchaholic.createWorld.toggle` | Munchaholic Mode |
| `munchaholic.createWorld.toggle.tooltip` | Every time you finish eating, one random attribute (size, jump, speed, health, gravity, reach…) is permanently buffed or debuffed. Changes stack. Saved with this world. Operators can change it later with /munchaholic on\|off. |
| `munchaholic.createWorld.rollMode` | Roll Mode |
| `munchaholic.createWorld.rollMode.tooltip.random` | Random: every bite changes a random attribute, up or down. Saved with this world. Operators can change it later with /munchaholic mode random\|recipes. |
| `munchaholic.createWorld.rollMode.tooltip.recipes` | Recipes: each food always changes the same attribute the same way in this world. Eat a food to discover what it does, and its tooltip shows it from then on. Saved with this world. Operators can change it later with /munchaholic mode random\|recipes. |
| `munchaholic.rollMode.random` | Random |
| `munchaholic.rollMode.recipes` | Recipes |

### `/munchaholic` feedback

| Key | Text |
|---|---|
| `munchaholic.command.on` | Munchaholic Mode is now ON for this world |
| `munchaholic.command.off` | Munchaholic Mode is now OFF for this world |
| `munchaholic.command.status.on` | Munchaholic Mode is ON in this world |
| `munchaholic.command.status.off` | Munchaholic Mode is OFF in this world |
| `munchaholic.command.mode.set` | `Roll Mode is now %s for this world` |
| `munchaholic.command.mode.status` | `Roll Mode is %s in this world` |
| `munchaholic.command.keepOnDeath.on` | Attribute changes are now kept on death in this world |
| `munchaholic.command.keepOnDeath.off` | Attribute changes are now lost on death in this world |
| `munchaholic.command.keepOnDeath.status.on` | Attribute changes are kept on death in this world |
| `munchaholic.command.keepOnDeath.status.off` | Attribute changes are lost on death in this world |
| `munchaholic.command.stats.header` | `%1$s — bites eaten: %2$s, recipes discovered: %3$s` |
| `munchaholic.command.stats.line` | `• %1$s %2$s %3$s` |
| `munchaholic.command.stats.none` | `%s has no attribute changes yet` |
| `munchaholic.command.reset.single` | `Reset the attribute changes of %s` |
| `munchaholic.command.reset.multiple` | `Reset the attribute changes of %s players` |
| `munchaholic.command.reset.recipes.single` | `%s forgot all discovered recipes` |
| `munchaholic.command.reset.recipes.multiple` | `%s players forgot all discovered recipes` |

### Roll messages and chat lines

| Key | Text |
|---|---|
| `munchaholic.message.rolled` | `✦ %1$s → %2$s %3$s %4$s` |
| `munchaholic.message.now` | `(now %s)` |
| `munchaholic.message.capped` | `✦ %1$s → %2$s %3$s (at the limit: %4$s)` |
| `munchaholic.message.nothing` | `✦ %s → nothing left to change` |
| `munchaholic.message.reset` | Your Munchaholic attribute changes were reset |
| `munchaholic.message.recipesReset` | Your discovered Munchaholic recipes were forgotten |
| `munchaholic.message.lostOnDeath` | You lost your Munchaholic attribute changes |
| `munchaholic.unit.percent` | `%s%%` |
| `munchaholic.unit.points` | `%s` |

### Recipe tooltip

| Key | Text |
|---|---|
| `munchaholic.tooltip.recipe` | `Munchaholic: %1$s %2$s` |
| `munchaholic.tooltip.undiscovered` | Munchaholic: ??? (eat it to find out) |
| `munchaholic.direction.up` | ▲ |
| `munchaholic.direction.down` | ▼ |

### Notification settings (client)

| Key | Text |
|---|---|
| `munchaholic.command.notify.sound` | `Roll sound: %s` |
| `munchaholic.command.notify.message` | `Roll message: %s` |
| `munchaholic.settings.title` | Munchaholic Settings |
| `munchaholic.settings.notifySound` | Roll sound |
| `munchaholic.settings.notifySound.tooltip` | Play a short sound when a bite changes one of your attributes. |
| `munchaholic.settings.notifyMessage` | Roll message |
| `munchaholic.settings.notifyMessage.tooltip` | Show the actionbar message (✦ food → attribute change) after every bite. |

The Create World buttons render the label and the value together (`Roll Mode: Random`, `Munchaholic Mode: ON`), so the label keys have no `%s`.

**Command feedback:** use short, whole sentences, the same as Enchantaholic. ON/OFF are part of the sentence (separate keys: `command.on` / `command.off` / `command.status.on` / `command.status.off`, and the same for keep-on-death), in the default command-feedback color. Mode names (`Random`, `Recipes`) are gold (`§6`). Changes (`/munchaholic on|off`, `/munchaholic mode random|recipes`, `/munchaholic keep-on-death on|off`, `/munchaholic reset …`) need permission level 2 (`LEVEL_GAMEMASTERS`) and broadcast to operators like vanilla game-rule changes. Reading (`/munchaholic`, `status`, bare `mode`, bare `keep-on-death`, `stats` for yourself) is open to everyone and replies only to the sender; `stats <player>` needs level 2 and also replies only to the sender. `/munchaholic-notify sound|message [on|off]` and `/munchaholic-notify status` reply only to the player who ran them.

**Roll message:** show it on the **actionbar** so snacking doesn't flood chat. Modded clients receive it in the `munchaholic:rolled` payload and apply their own settings; vanilla clients get `sendOverlayMessage` plus a sound packet.

Exact format: `✦ <food> → <attribute> <signed change> (now <value>)`. Examples:
- `✦ Carrot → Scale -8% (now 92%)`, where `-8%` is red.
- `✦ Steak → Jump Strength +10% (now 110%)`, where `+10%` is green.
- `✦ Golden Apple → Max Health +2 (now 22)`, where `+2` is green.
- `✦ Bread → Gravity -10% (now 90%)`, where `-10%` is green (less gravity is a buff).
- Recipes mode, at the cap: `✦ Carrot → Scale -8% (at the limit: 20%)`, with `-8%` and `20%` gray.
- Nothing can change at all: `✦ Carrot → nothing left to change`.

Argument details:
- `%1$s` = the food's hover name (`stack.getHoverName()`), styled gold `§6`. The `✦` keeps the series look.
- `%2$s` = the vanilla attribute name (`Component.translatable("attribute.name." + key)`, shipped by vanilla in every language, for example Scale, Jump Strength, Speed, Max Health, Gravity, Block Interaction Range), styled white `§f`.
- `%3$s` = the signed change. It is green `§a` if the roll helps the player and red `§c` if it hurts. Use the effect, not the sign: more Gravity is a debuff, so `Gravity +10%` is red.
- `%4$s` = `munchaholic.message.now`, styled gray `§7`.
- Build it with `Component.translatableWithFallback("munchaholic.message.rolled", "✦ %1$s → %2$s %3$s %4$s", food, attr, change, now)` rather than concatenating strings; the fallback must equal en_us (LangFileTest).
- Sound: one amethyst chime (`AMETHYST_BLOCK_CHIME`, volume 0.5): pitch 1.5 for a buff, 0.8 for a debuff, 1.1 when capped or nothing can change. Played only when the roll sound is on; vanilla clients get it as a sound packet.

The 🥕-style emoji aren't in Minecraft's font, so they don't go in-game. Use the ✦ instead.

## Number formatting
`core/Numbers.java` and `core/DisplayUnit.java`. The unit is a translation key (`munchaholic.unit.percent` = `%s%%`, `munchaholic.unit.points` = `%s`), so it stays localizable.

- **Percent-of-base attributes** (Scale, Gravity, Jump Strength, Speed, Sneaking Speed, Attack Speed, Fall Damage Multiplier, both Interaction Ranges, Burning Time): signed whole-percent change (`+10%`, `-8%`); `now` is a percent of the player's base value, `100%` = normal.
- **Percent-point attributes** (Knockback Resistance, Water Movement Efficiency): signed whole-percent change; `now` is the value × 100 (`0%` = vanilla).
- **Point attributes** (Step Height, Attack Damage, Max Health, Armor, Safe Fall Distance, Mining Efficiency, Oxygen Bonus, Luck): signed amount with up to 2 decimals, trailing zeros dropped (`+2`, `+0.5`); `now` is the actual value. Max Health is in health points, not hearts.
- ASCII `+`/`-` and a `.` decimal point in every language. If rounding would show 0, up to 3 decimals are used. Zero has no sign.
