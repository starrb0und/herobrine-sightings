# Herobrine Sightings

A Minecraft server plugin that, on rare occasions, spawns a Herobrine figure at the edge of a player's vision. He stays for a few seconds, or vanishes the moment the player turns to look at him.

> **Status:** concept stage. No target Minecraft version or server platform has been chosen yet.

## Planned behaviour

- **Rare trigger** – each online player has a small, configurable chance per interval of a sighting.
- **Peripheral placement** – Herobrine appears off to the side of the player's view direction, at a distance, on a spot with line of sight to the player.
- **Vanishes when noticed** – he despawns as soon as the player's view direction turns towards him, or after a short timeout, whichever comes first.
- **Per-player** – ideally only the targeted player sees him.

## Open decisions

- Target Minecraft version(s)
- Server platform (Paper, Spigot, Folia, ...)
- How the model is rendered (fake player with a Herobrine skin via packets, display entities, a resource-pack model, ...)
- Build tool (Gradle or Maven)

## Configuration ideas

| Option | Purpose |
| --- | --- |
| `chance` | Probability of a sighting per check |
| `check-interval` | How often each player is rolled for a sighting |
| `min-distance` / `max-distance` | How far away he appears |
| `peripheral-angle` | Angle range off the player's view direction where he may spawn |
| `look-threshold` | How directly the player must look before he vanishes |
| `max-duration` | Timeout before he vanishes on his own |
| `cooldown` | Minimum time between sightings for one player |
| `enabled-worlds` | Worlds where sightings can happen |
