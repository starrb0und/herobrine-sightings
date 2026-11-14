# Herobrine Sightings

A server-side Fabric mod for Minecraft 26.2. On rare occasions, Herobrine appears at the edge of a player's vision. He stays for a few seconds, or vanishes the moment the player turns to look at him.

Players do not need to install anything: it works with vanilla clients.

> **Status:** early development. It builds, but has not been tested in game yet.

## How it works

- Every `checkIntervalTicks`, each online player has a `chance` of a sighting.
- Herobrine is placed off to the side of the player's view, on solid ground, where the player has line of sight to him.
- He is a mannequin entity that is never added to the world. Its packets are sent only to the haunted player, so nobody else sees him and nothing can interact with him.
- He vanishes when the player looks towards him, gets too close, changes dimension, or when the timeout runs out.

## Commands

Both need operator (gamemaster) permission.

| Command | Purpose |
| --- | --- |
| `/herobrine spawn` | Force a sighting for yourself, for testing |
| `/herobrine reload` | Re-read the config file |

## Configuration

`config/herobrine.json` is created on first start.

| Option | Default | Purpose |
| --- | --- | --- |
| `checkIntervalTicks` | `200` | How often each player is rolled for a sighting (20 ticks = 1 second) |
| `chance` | `0.004` | Probability of a sighting on each roll |
| `cooldownSeconds` | `600` | Minimum time between sightings for one player |
| `minDistance` / `maxDistance` | `16` / `40` | How far away he appears, in blocks |
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
