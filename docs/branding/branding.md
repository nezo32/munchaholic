# Munchaholic — Branding

> This file is the branding source for Munchaholic. The logo, icon, palette and every player-facing string below should match it. Change it here first.

**Tagline:** *Take a bite. Roll a stat. Never stop.*
Alt: *Take a bite. Change a stat. Never stop.* / *One more carrot can't hurt. Probably.*

## CurseForge description

> **Munchaholic** turns every snack into a gamble. Each time you **finish eating something**, one of your **player attributes is permanently buffed or debuffed**: size, jump, speed, max health, gravity, reach and more. Eat a carrot and shrink 8%. Eat a steak and jump like a rabbit. Eat a cookie and suddenly you can reach across the room. Keep eating, the changes stack, and nothing ever resets on its own. The mode is a toggle on the world-creation screen, saved with the world, and operators can flip it any time with `/munchaholic on|off`, so you can switch it on in any world. Pick a **Roll Mode**: *Random* rolls a new stat with every bite, and *Recipes* ties each food to its own stat for the whole world, so you can work out what each food does. Each player can mute the roll sound, the actionbar message, or both. It's a **Fabric mod for Java 26.2–26.3**.

## Features
- 🍖 **Every bite counts:** finishing any food rolls one random attribute, up or down, for good.
- 🎲 **Buff or debuff:** each roll can go either way. Grow, shrink, float, sink, sprint or crawl. The chaos is the point.
- 📈 **It stacks:** changes build on each other bite after bite. Every attribute has a safety cap, so you can't shrink to nothing or fall through the world.
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

In-game text: use `§d` (light purple) for the ✦ and `§6` (gold) for the food name. The attribute name is `§f` (white). The change is `§a` (green) for a buff and `§c` (red) for a debuff. The `(now …)` part is `§7` (gray).

## Player-facing strings (en_us)
| Key | Text |
|---|---|
| `munchaholic.createWorld.toggle` | Munchaholic Mode |
| `munchaholic.createWorld.toggle.tooltip` | Every time you finish eating, one random attribute (size, jump, speed, health, gravity, reach…) is permanently buffed or debuffed. Changes stack. Saved with this world. Operators can change it later with /munchaholic on\|off. |
| `munchaholic.createWorld.rollMode` | Roll Mode: %s |
| `munchaholic.createWorld.rollMode.random` | Random |
| `munchaholic.createWorld.rollMode.recipes` | Recipes |
| `munchaholic.createWorld.rollMode.tooltip` | Random: every bite rolls any attribute. Recipes: each food always changes the same attribute in this world, so you can learn what each food does. Saved with this world. Operators can change it later with /munchaholic mode random\|recipes. |
| `munchaholic.command.on` | Munchaholic Mode is now ON for this world |
| `munchaholic.command.off` | Munchaholic Mode is now OFF for this world |
| `munchaholic.command.status.on` | Munchaholic Mode is ON in this world |
| `munchaholic.command.status.off` | Munchaholic Mode is OFF in this world |
| `munchaholic.command.mode.set` | Roll Mode is now %s for this world |
| `munchaholic.command.mode.status` | Roll Mode: %s |
| `munchaholic.message.rolled` | `✦ %1$s → %2$s %3$s %4$s` |
| `munchaholic.message.now` | (now %s) |
| `munchaholic.settings.title` | Munchaholic Settings |
| `munchaholic.settings.notifySound` | Roll sound |
| `munchaholic.settings.notifySound.tooltip` | Play a short sound when a bite changes one of your attributes. |
| `munchaholic.settings.notifyMessage` | Roll message |
| `munchaholic.settings.notifyMessage.tooltip` | Show the actionbar message (✦ food → attribute change) when a bite changes one of your attributes. |
| `munchaholic.command.notify.sound` | Roll sound: %s |
| `munchaholic.command.notify.message` | Roll message: %s |

**Command feedback:** use short, whole sentences, the same as Enchantaholic. The state word is a styled argument: `ON` is green (`§a`) and `OFF` is red (`§c`). Everything else uses the default command-feedback color. Mode names (`Random`, `Recipes`) are gold (`§6`). Operator commands (`/munchaholic on|off|status`, `/munchaholic mode random|recipes|status`) broadcast to operators like vanilla game-rule changes. `/munchaholic-notify sound|message [on|off]` replies only to the player who ran it.

**Roll message:** show it on the **actionbar** (`player.displayClientMessage(msg, true)`) so snacking doesn't flood chat.

Exact format: `✦ <food> → <attribute> <signed change> (now <value>)`. Examples:
- `✦ Carrot → Scale -8% (now 92%)`, where `-8%` is red.
- `✦ Steak → Jump Strength +12% (now 112%)`, where `+12%` is green.
- `✦ Golden Apple → Max Health +2 (now 22)`, where `+2` is green.

Argument details:
- `%1$s` = the food's hover name (`stack.getHoverName()`), styled gold `§6`. A leading `✦` in `§d` keeps the series look.
- `%2$s` = the vanilla attribute name (`Component.translatable(attribute.value().getDescriptionId())`, for example Scale, Jump Strength, Speed, Max Health, Gravity, Block Interaction Range), styled white `§f`.
- `%3$s` = the signed change. It is green `§a` if the roll helps the player and red `§c` if it hurts. Use the effect, not the sign: more Gravity is a debuff, so `Gravity +10%` is red.
- `%4$s` = `munchaholic.message.now`, styled gray `§7`.
- Build it with `Component.translatable("munchaholic.message.rolled", food, attr, change, now)` rather than concatenating strings.
- Optional: a short eat-then-chime sound (`PLAYER_BURP` then `NOTE_BLOCK_CHIME` at low volume). Play it only when the roll sound is on.

The 🥕-style emoji aren't in Minecraft's font, so they don't go in-game. Use the ✦ instead.

## Number formatting
- **Percent attributes** (scale, speed, jump, gravity, reach, and anything multiplied): show the change as a signed whole percent (`+12%`, `-8%`). Show `now` as a percent of the vanilla base value, where `100%` means normal.
- **Point attributes** (Max Health, Armor, and anything added): show the change as a signed amount (`+2`, `-1`) and `now` as the actual value. Max Health is in health points, not hearts.
- Use ASCII `+` / `-` so every font renders the sign. Round to a whole number. If rounding would show `0%`, show one decimal place instead (`+0.5%`).
