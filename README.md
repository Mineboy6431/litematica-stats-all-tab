# Litematica Stats (All Tab) - fork

Fork of [OC0v0/litematica-stats](https://github.com/OC0v0/litematica-stats) (MIT).
Adds a fourth tab **All** to Litematica's material list that shows block materials, entities and
container contents together as three collapsible sections. The original three tabs are unchanged.

The All tab is available in every version port of this repository
(1.18.2, 1.19.4, 1.20.1, 1.21.1, 1.21.11, 26.2 and 26.3). The mod id is `litematica-stats-all`;
it replaces the original mod and must not be installed together with it.
The GitHub Actions workflow *Build all versions* builds every port and attaches the jars as artifacts.

### Replace an item in all containers (1.21.11, 26.1, 26.2, 26.3)

In the **All** and **Contents** (summed up) lists you can replace an item type in every container of the
loaded schematic at once: hold the new item in your main hand, point at the item row you want to replace and
press the hotkey (default `Ctrl+R`). All stacks of that item in all chests, barrels, shulker boxes and
container entities become the held item; slots and counts are kept (the count is capped at the new item's
maximum stack size). The change applies to the loaded schematic in memory, until it is reloaded from disk.

The hotkey is set in `config/litematica-stats-all.json` (`"replaceHotkey": "CTRL+R"`, modifiers `CTRL`, `SHIFT`,
`ALT` plus a key name such as `R`, `F6` or `KP_ADD`); the file is created on first use and read at game start.

### Fill containers when they are placed (1.21.11, 26.1, 26.2, 26.3)

When you place a container block that the loaded schematic contains with items, the stored items are put in
automatically:

- **Creative mode:** the placed item already carries the stored items, so nothing has to be in your inventory.
- **Survival mode:** the container is opened right after placing, the items are moved from your inventory into
  the slots given by the schematic, and the container is closed again. Items you do not carry are skipped and
  reported. This does not run while you sneak, and only for chests, barrels, shulker boxes, hoppers,
  dispensers and droppers.

Set `"autoFill"` in `config/litematica-stats-all.json` to `both` (default), `creative`, `survival` or `off`.
`"clicksPerTick"` (1 to 10, default 2) limits how many inventory clicks are sent per tick. Some servers do not
allow automated inventory clicks; use `off` or `creative` there.

### Highlight missing or wrong items in a container (1.21.11, 26.1, 26.2, 26.3)

When you open a chest, barrel, shulker box, hopper, dispenser or dropper that is part of the loaded schematic,
every slot whose items differ from the schematic (missing, wrong or extra item, or a wrong count) is tinted.
The color is `"mismatchColor"` in `config/litematica-stats-all.json`: `#RRGGBB` (drawn 50% opaque) or
`#AARRGGBB`; the default is black (`#000000`). `"verifyOverlay": false` switches the highlight off.
Only containers you open can be checked, because the game does not send the contents of closed containers.

### Additional Minecraft versions

The workflow also builds the code of the nearest port for further Minecraft versions, against the
Litematica, MaLiLib and Fabric API builds that Modrinth lists for that version
(a version is skipped when Modrinth has no Litematica build for it).
`.github/adapt/adapt.py` applies the small API differences of each version before the build.

| Minecraft | Code taken from | Adaptations |
|---|---|---|
| 1.18, 1.18.1 | `litematica-stats-1.18.2` | registry access |
| 1.19, 1.19.1, 1.19.2 | `litematica-stats-1.19.4` | registries, item rendering, `BlockPos` |
| 1.19.3 | `litematica-stats-1.19.4` | item rendering, `BlockPos` |
| 1.20.2, 1.20.3, 1.20.4 | `litematica-stats-1.20.1` | none |
| 1.20.5, 1.20.6 | `litematica-stats-1.21.1` | MaLiLib GUI helpers, item tooltip |
| 1.21.2, 1.21.3, 1.21.4 | `litematica-stats-1.21.1` | MaLiLib calls take the `GuiGraphics`, registry lookups |
| 26.1 | `litematica-stats-26.2` | none |

Not included: 1.21.5 to 1.21.10. These versions changed the NBT and GUI APIs and need a real port.

These builds are experimental: they are compile-checked by the workflow, but not tested in the game.

The original README follows.

---

# Litematica Stats

A client-side Fabric addon that extends **Litematica's Material List** with entity and container-content statistics.

## Features

- **Three tabs in the material list screen**
  - Blocks — the original Litematica material list.
  - Entities — counts entities by type with spawn egg icons (informational only, never added to material totals).
  - Container Contents — scans block containers and container entities, shows items as slot-grid previews.
- Double chests are merged into one container; containers with identical contents can be merged with an ×N badge.
- Vanilla container GUI layouts and full capacities are respected (furnace, hopper, dispenser, brewing stand, bookshelf, campfire, beacon, chest/barrel/shulker, …).
- Hover slots for the vanilla item tooltip, compatible with Tweakeroo/Minihud shulker previews.
- Ignore individual containers or whole categories; ignored items collect in a section at the bottom.
- Unified item view, hide-empty toggle, search filtering and sortable columns.
- Smooth pixel-based scrolling.

## Version ports

One standalone Gradle project per Minecraft version:

| Directory | Minecraft | Build JVM |
|---|---|---|
| `litematica-stats` | 1.21.11 | JDK 21 |
| `litematica-stats-1.21.1` | 1.21.1 | JDK 21 |
| `litematica-stats-1.20.1` | 1.20.1 | JDK 21 |
| `litematica-stats-1.19.4` | 1.19.4 | JDK 21 |
| `litematica-stats-1.18.2` | 1.18.2 | JDK 21 |
| `litematica-stats-26.2` | 26.2 | JDK 26 |
| `litematica-stats-26.3` | 26.3 | JDK 26 |

Every project uses Gradle 9.5 and Loom 1.17.20. Third-party mod jars are **not** included in this repository; place the matching Litematica + MaLiLib jars into each project's `libs/` directory before building:

| Project | Required jars in `libs/` |
|---|---|
| `litematica-stats` | `litematica-fabric-1.21.11-0.26.16.jar`, `malilib-fabric-1.21.11-0.27.20.jar`, `conditional-mixin-fabric-0.6.4.jar` |
| `litematica-stats-1.21.1` | `litematica-fabric-1.21-0.19.61.jar`, `malilib-fabric-1.21-0.21.10.jar` |
| `litematica-stats-1.20.1` | `litematica-fabric-1.20.1-0.15.4.jar`, `malilib-fabric-1.20.1-0.16.3.jar` |
| `litematica-stats-1.19.4` | `litematica-fabric-1.19.4-0.14.7.jar`, `malilib-fabric-1.19.4-0.15.4.jar` |
| `litematica-stats-1.18.2` | `litematica-fabric-1.18.2-0.11.7.jar`, `malilib-fabric-1.18.2-0.12.1.jar` |
| `litematica-stats-26.2` | `litematica-fabric-26.2-0.28.8.jar`, `malilib-fabric-26.2-0.29.6.jar`, `conditional-mixin-fabric-0.6.4.jar` |
| `litematica-stats-26.3` | `litematica-fabric-26.3-0.29.1.jar`, `malilib-fabric-26.3-0.30.2.jar`, `conditional-mixin-fabric-0.6.4.jar` |

For the 26.x ports, patch `Fabric-Loom-Version` in the jars' `META-INF/MANIFEST.MF` to `1.17.20`.

## Building

```bash
cd litematica-stats          # or any version port
JAVA_HOME=/path/to/jdk gradle build
```

The remapped jar is copied to `../output/` automatically.

## Requirements

- Fabric Loader, Fabric API
- [Litematica](https://modrinth.com/mod/litematica)
- [MaLiLib](https://modrinth.com/mod/malilib)

## Links

- Modrinth: https://modrinth.com/mod/litematica-stats

## License

MIT — see [LICENSE](LICENSE).
