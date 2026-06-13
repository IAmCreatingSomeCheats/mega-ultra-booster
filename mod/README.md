# TurboBoost (Fabric mod)

The in-game half of MEGA Ultra Booster, for **Minecraft 1.21.11 / Fabric**.

* One-key performance profile (fast graphics, clouds off, minimal particles, no
  entity shadows, tuned render + entity distance, vsync off).
* Live **FPS / frame-time / RAM** overlay.
* **Dynamic FPS** — throttles framerate while the window is unfocused (no mixin,
  so it survives MC updates).
* **Smart Server Switcher** — one key disconnects and reconnects you to the
  quietest server in a category (the desktop app picks it; offline it just cycles
  your local list).
* **Live link** to the desktop app over `127.0.0.1:38910`.

> ✅ Built & verified against real `1.21.11` + yarn `1.21.11+build.6` mappings →
> `build/libs/turboboost-1.0.0.jar` (Gradle 9.5.1, Loom 1.14.10).

## Build

From this `mod/` folder:

```bash
./gradlew build          # macOS/Linux/Git-Bash
gradlew.bat build        # Windows cmd/PowerShell
```

The Gradle wrapper downloads everything it needs, including a **JDK 21 toolchain**
to compile against (via the Foojay resolver) — you don't need to install JDK 21
yourself even on a newer JDK. The finished mod is:

```
build/libs/turboboost-1.0.0.jar
```

## Install

1. Install **Fabric Loader** (0.19.3+) for **Minecraft 1.21.11**.
2. Put these in `.minecraft/mods`:
   * `turboboost-1.0.0.jar` (this mod)
   * **Fabric API** — https://modrinth.com/mod/fabric-api
   * **Sodium** + **Lithium** *(strongly recommended — they do the heavy
     engine-level optimization TurboBoost intentionally doesn't reinvent)*
3. Launch. The overlay appears top-left and shows `LINK ✔` once the desktop app
   is open.

## Keybinds (rebindable in Options → Controls → TurboBoost)

| Key | Action |
|-----|--------|
| **F6** | Toggle the FPS/RAM overlay |
| **F7** | Apply the boost profile now |
| **K**  | Quick-switch to the quietest server in your category |

## Config (auto-created in `.minecraft/config/`)

* `turboboost.json` — overlay, dynamic-FPS, link host/port, and the
  `quickSwitchCategory` the **K** key targets (default `Casual/Beginner`).
* `turboboost-servers.json` — offline fallback server list (the app's list is
  authoritative when connected).

## Updating to a newer Minecraft version

Edit `gradle.properties` and bump `minecraft_version`, `yarn_mappings`,
`loader_version`, and `fabric_version` to a matching set from
**https://fabricmc.net/develop**, then rebuild. The only code that tends to need
attention across versions is the `ConnectScreen.connect(...)` call in
[`ServerSwitcher.java`](src/main/java/com/megaultra/turboboost/server/ServerSwitcher.java)
(its signature changes occasionally — check on https://linkie.shedaniel.dev).
