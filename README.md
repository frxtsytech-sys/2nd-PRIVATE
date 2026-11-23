# OblivionCombat (Fabric, Minecraft 1.21.11) - Stage 1+2

Fabric mod: premium PvP weapons with animated abilities. Drop the jar into `mods/` of a modpack, a dedicated server, or a singleplayer instance (requires Fabric API). It adds no new blocks/items/registries, so players joining a server that has it do not need it installed.

Included in this stage: the weapon/ability/animation/combat framework and the full **VOID REAVER** with the **Blackhole**.
Planned next stages: the other 5 swords, the 5 axes, remaining kill effects, GUI.

## Status (read this)
This source has **not been compiled or run**. It was written without access to the Minecraft/Fabric jars. Expect a few
mapping-related compile errors on first build. The calls most likely to need a one-line fix are all isolated in
`src/main/java/com/oblivioncombat/util/Compat.java` (world lookup, admin check, teleport, damage, velocity).
Paste any compiler errors back and they get fixed.

## Build without installing anything
Push this folder to a GitHub repo; the included workflow (`.github/workflows/build.yml`) builds the jar and attaches it as an artifact under the Actions tab.

## Build
Requirements: JDK 21 and Gradle 9.x (or generate a wrapper with `gradle wrapper`).
```
gradle build
```
Output: `build/libs/OblivionCombat-0.1.0.jar`. Versions are in `gradle.properties`; check https://fabricmc.net/develop for newer ones.

## Modpack / instance install
Put the jar in the instance's `mods/` folder with Fabric Loader 0.18+ and Fabric API for 1.21.11. Works on CurseForge/Modrinth/Prism instances. In singleplayer, use `/oblivion give @s void_reaver` (cheats on).

## Install
1. Fabric Loader 0.18+ and Fabric API for 1.21.11 on the server.
2. Put `OblivionCombat-0.1.0.jar` in `mods/`.
3. Start once. Config is created at `config/oblivioncombat/config.json`.

## Void Reaver controls
| Input | Ability |
|---|---|
| Right-click | Void Slash (crescent projectile) |
| Sneak + Right-click | Blackhole; press again while active = Void Collapse (early detonation) |
| Sneak + Left-click on an entity | Dimensional Rift (teleport behind target, strike) |
| Killing something | Void Execution (visual implosion) |

Left-click on *air* and double-click are not reported to the server by vanilla clients, so they are not offered.

## Commands
| Command | Who | Description |
|---|---|---|
| /oblivion | all | help |
| /oblivion weapons | all | list weapons |
| /oblivion abilities | all | abilities, triggers, cooldowns |
| /oblivion version | all | version |
| /oblivion give <player> <weapon> | op level 2 | give a weapon |
| /oblivion cooldown <player> [weapon] | op level 2 | reset cooldowns |
| /oblivion reload | op level 2 | reload config (keeps old config if the file is invalid) |
| /oblivion debug | op level 2 | diagnostics |

## Permissions
Fabric has no built-in permission nodes. Admin commands require operator level 2. Everyone can use weapons they hold.
`creativeBypassCooldown` replaces `oblivion.bypass.cooldown`. LuckPerms-style nodes are not implemented yet.

## Configuration (config.json)
JSON, not YAML (Fabric ships Gson, not a YAML library). Missing keys use defaults; `/oblivion reload` applies changes.
Weapons you already hold keep their old damage/speed attributes until re-given.

Global: `effectsEnabled`, `quality` (LOW/MEDIUM/HIGH/ULTRA), `particleDensity`, `maxParticlesPerTick`,
`maxActiveAnimations`, `allowPvp`, `creativeBypassCooldown`, `showActionBar`, `showTitles`, `announceRareWeapons`,
`comboWindowTicks`, `combatTagSeconds`, `assistWindowSeconds`, `disabledWorlds` (e.g. `["minecraft:the_end"]`), `messages.*` (& colour codes).

`voidReaver`: `enabled`, `rarity` (COMMON..DIVINE), `attackDamage`, `attackSpeed`.
`voidSlash`: `enabled`, `cooldownSeconds`, `damage`, `range`, `speed`, `size`, `tiltDegrees`, `knockback`.
`blackhole`: `enabled`, `cooldownSeconds`, `radius`, `durationSeconds`, `pullStrength`, `damagePerSecond`, `explosionDamage`,
`explosionStrength`, `maxEntities`, `castDistance`, `affectPlayers`, `affectMobs`.
`rift`: `enabled`, `cooldownSeconds`, `damage`, `behindDistance`.

## Performance design
- One server-tick hook drives every animation (no per-effect repeating tasks, no extra entities).
- Hard per-tick particle budget plus quality/density scaling; cap on simultaneous animations.
- Blackhole affects at most `maxEntities` entities and damage is applied every 10 ticks.
- Animations are cancelled on disconnect and cleared on shutdown; the only entity side effect is a 6-tick Slowness refresh.
- Disk I/O happens only at startup and reload.

## Not done yet / honest limits
Other swords, axes, GUI, remaining kill effects, ultimate meter. Nothing here has been tested in-game.
