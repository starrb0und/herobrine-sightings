# Herobrine Sightings

A server-side Fabric mod for Minecraft 26.2. On rare occasions, Herobrine appears at the edge of a player's vision. He stays for a few seconds, or vanishes the moment the player turns to look at him.

Players do not need to install anything: it works with vanilla clients.

## How it works

- Each player sees Herobrine about once per Minecraft week (7 in-game days, 2 hours 20 minutes of play) on average. Sightings are random, not on a schedule.
- The odds go up during a thunderstorm and while the player is underground in the dark, and drop sharply while another player is nearby.
- Herobrine is placed at least 45 blocks away, off to the side of the player's view, on solid ground, where the player has line of sight to him. If no such spot exists, the sighting is retried for up to five minutes.
- He is a mannequin entity that is never added to the world. Its packets are sent only to the haunted player, so nobody else sees him and nothing can interact with him.
- He vanishes when the player looks towards him, gets too close, changes dimension, or when the timeout runs out.

Players whose **Entity Distance** video setting is below 100% may not see him at 45 blocks or more, because the client stops drawing player-sized entities beyond 64 blocks times that setting.

## Herobrine's altars

Small altars generate in newly created chunks of the Overworld: a ring of gold blocks around a soul fire, with a redstone torch on each side, on a mossy cobblestone footing. They use the same placement grid as ocean monuments, thinned out so they are about as hard to come across, and appear in most land biomes.

`/locate structure herobrine:altar` finds the nearest one. Chunks generated before the mod was installed will not contain altars.

The layout is defined in `tools/GenerateAltar.java`; run `java tools/GenerateAltar.java` from the project root to rebuild the template after editing it. Rarity is set in `src/main/resources/data/herobrine/worldgen/structure_set/altars.json`.

## Commands

All need operator (gamemaster) permission.

| Command | Purpose |
| --- | --- |
| `/herobrine haunt <player>` | Force a sighting for that player |
| `/herobrine spawn` | Force a sighting for yourself |
| `/herobrine reload` | Re-read the config file |

## Configuration

`config/herobrine.json` is created on first start. A file from an older version of the mod is reset to the new defaults, and the old one is kept as `herobrine.json.old`.

| Option | Default | Purpose |
| --- | --- | --- |
| `sightingsPerMinecraftWeek` | `1.0` | Average sightings per player per Minecraft week, before the multipliers below |
| `thunderstormMultiplier` | `4.0` | Odds multiplier during a thunderstorm |
| `darkCaveMultiplier` | `3.0` | Odds multiplier while underground in the dark |
| `darkCaveMaxLight` | `4` | Light level at or below which an underground player counts as in the dark |
| `groupMultiplier` | `0.15` | Odds multiplier while another player is within `groupRadius` |
| `groupRadius` | `24` | How close another player must be to count as company, in blocks |
| `checkIntervalTicks` | `200` | How often each player is rolled for a sighting (20 ticks = 1 second) |
| `cooldownSeconds` | `600` | Minimum time between sightings for one player |
| `minDistance` / `maxDistance` | `45` / `60` | How far away he appears, in blocks |
| `minAngle` / `maxAngle` | `30` / `50` | How far off the centre of the player's view he appears, in degrees |
| `lookThresholdDegrees` | `15` | He vanishes once the player's view is within this angle of him |
| `maxDurationTicks` | `100` | Timeout before he vanishes on his own |
| `vanishDistance` | `8` | He vanishes if the player gets closer than this |
| `skinPlayer` | `MHF_Herobrine` | Player account whose skin he wears |

## Building

Requires JDK 25.

```
./gradlew build
```

The mod jar is written to `build/libs/`. It needs [Fabric API](https://modrinth.com/mod/fabric-api) on the server.
