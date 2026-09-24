# Munchaholic: CurseForge page

The text for the Munchaholic project page on CurseForge. It mirrors the
[Enchantaholic page](https://www.curseforge.com/minecraft/mc-mods/enchanaholic) section for section.

## Project settings

| Field | Value |
|---|---|
| Name | `Munchaholic` |
| Summary | `Every food you finish changes a random attribute, up or down, for good. The changes stack.` |
| Class | Mods |
| Category | Miscellaneous (the only one, like Enchantaholic) |
| License | MIT License (© 2026 nezo) |
| Source link | `https://github.com/nezo32/munchaholic` (no Issues, Wiki, Discord or Donate link) |
| Avatar | `docs/branding/curseforge_logo.png` |
| Environment | Client & Server |
| Files | Uploaded by `release.yml`: `munchaholic-X.Y.Z.jar`, game versions `26.2, 26.3, Fabric, Java 25, Client, Server` |

## Description

Paste everything between the two markers into the CurseForge description editor (Markdown mode). The description
has no images, like Enchantaholic's.

<!-- BEGIN CURSEFORGE DESCRIPTION -->

# Munchaholic

**Munchaholic** turns every snack into a gamble. Each time you finish eating something, **one of your player attributes is permanently buffed or debuffed**: size, gravity, jump, speed, max health, reach and more. Keep eating and the **changes stack**: shrink to a fifth of your size or grow almost six times taller, float on 20% gravity, sprint at triple speed. Clear out a pantry, come out tiny, with 30 hearts and a jump that clears a house.

The mode is a **Munchaholic Mode** button on the Create World screen, right below Difficulty, and it's **ON by default**. A second button picks the **Roll Mode**. Easy, Normal, Hard and Hardcore work exactly as in vanilla.

## Features

- 🍖 **Every bite counts:** every food you finish rolls once, including each slice of cake and foods from other mods and datapacks. Potions, milk and ominous bottles don't count.
- 🎲 **Buff or debuff:** one of 20 attributes, up or down, 50/50. Less gravity, softer landings and shorter burning count as buffs.
- 📈 **It stacks, for good:** every roll moves an attribute one step, and the steps add up bite after bite. Changes stay when you log out and, by default, when you die.
- 🛡️ **Safety caps:** every attribute has a limit (table below), so you can't shrink to nothing, lose your last heart or float away for good.
- 📖 **Two Roll Modes:** *Random* rolls any attribute with every bite. *Recipes* gives each food one fixed effect per world: eat it to discover what it does, and its tooltip shows it from then on.
- 🌍 **Per-world settings:** saved inside the world. Operators can switch them later with `/munchaholic on|off|status`, `/munchaholic mode random|recipes` and `/munchaholic keep-on-death on|off`.
- 📊 **Check your build:** `/munchaholic stats` lists your bites, discovered recipes and every changed attribute. Operators can undo it all with `/munchaholic reset`.
- 🔕 **Notification settings:** every roll shows a short actionbar message with a soft chime. Turn the sound, the message or both off from Mod Menu or with `/munchaholic-notify`.
- 🧑‍🤝‍🧑 **Multiplayer ready:** works on dedicated servers, with notification settings per player. In English and Russian.
- 🎮 **Fair to play with:** Creative and Spectator never trigger rolls, and fake players from other mods (auto-feeders) don't farm attributes.

## Attributes and caps

| Attribute | Per bite | Range | Buff is |
|---|---|---|---|
| **Scale** | 8% | 20% – 596% | higher |
| **Gravity** | 10% | 20% – 300% | lower |
| **Jump Strength** | 10% | 50% – 300% | higher |
| **Step Height** | 0.2 | 0.2 – 2.6 blocks | higher |
| **Speed** | 8% | 36% – 300% | higher |
| **Sneaking Speed** | 10% | 50% – 330% | higher |
| **Attack Damage** | 0.5 | 0.5 – 10 | higher |
| **Attack Speed** | 10% | 40% – 310% | higher |
| **Max Health** | 2 | 2 – 60 (1 – 30 hearts) | higher |
| **Armor** | 1 | 0 – 20 | higher |
| **Knockback Resistance** | 10% | -50% – 100% | higher |
| **Safe Fall Distance** | 1 | 1 – 23 blocks | higher |
| **Fall Damage Multiplier** | 10% | 10% – 300% | lower |
| **Block Interaction Range** | 10% | 30% – 300% | higher |
| **Entity Interaction Range** | 10% | 40% – 300% | higher |
| **Mining Efficiency** | 2 | 0 – 40 | higher |
| **Oxygen Bonus** | 1 | 0 – 20 | higher |
| **Water Movement Efficiency** | 10% | 0% – 100% | higher |
| **Burning Time** | 10% | 10% – 300% | lower |
| **Luck** | 1 | -10 – 10 | higher |

Percentages are relative to normal (100%). Caps count only your base value plus Munchaholic's own change: armor and potions don't count. A roll that would pass a cap rerolls another attribute in Random mode, and does nothing in Recipes mode (the message says it's at the limit).

## Requirements

- Minecraft Java **26.2–26.3**
- Fabric Loader 0.19.5+ and Fabric API
- Java 25
- Mod Menu is optional (notification settings screen)
- Install on both the server and the clients

## More from the -aholic series

Like this? Try **[Enchantaholic](https://www.curseforge.com/minecraft/mc-mods/enchanaholic)**: every block you break enchants a random item.

<!-- END CURSEFORGE DESCRIPTION -->
