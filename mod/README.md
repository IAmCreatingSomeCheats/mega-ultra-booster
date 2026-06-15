# TurboBoost (Fabric mod)

The in-game half of MEGA Ultra Booster — for **Minecraft 1.21.1 – 1.21.11 / Fabric**.

* One-key performance profile (fast graphics, clouds off, minimal particles, no
  entity shadows, tuned render + entity distance, vsync off).
* Live **FPS / frame-time / RAM** overlay.
* **Dynamic FPS** — throttles framerate while the window is unfocused (no mixin,
  so it survives MC updates).
* **Smart Server Switcher** — one key disconnects and reconnects you to the
  quietest server in a category (the desktop app picks it; offline it just cycles
  your local list).
* **Live link** to the desktop app over `127.0.0.1:38910`.

> ✅ Built & verified against real mappings for **1.21.1, 1.21.4, 1.21.8, and
> 1.21.11** (Gradle 9.5.1, Loom 1.14.10). The mod has **no mixins**, so it stays
> robust across MC updates.

## Supported versions

| Minecraft | yarn | Fabric API | jar |
|-----------|------|-----------|-----|
| 1.21.1  | `1.21.1+build.3`  | `0.116.12+1.21.1`  | `turboboost-1.0.0+mc1.21.1.jar` |
| 1.21.4  | `1.21.4+build.8`  | `0.119.4+1.21.4`   | `turboboost-1.0.0+mc1.21.4.jar` |
| 1.21.8  | `1.21.8+build.1`  | `0.136.1+1.21.8`   | `turboboost-1.0.0+mc1.21.8.jar` |
| 1.21.11 | `1.21.11+build.6` | `0.141.4+1.21.11`  | `turboboost-1.0.0+mc1.21.11.jar` |

## Build

From this `mod/` folder:

```powershell
# one specific version:
./gradlew build -Pmcver=1.21.8       # (defaults to 1.21.11 if omitted)

# every supported version into ./dist:
./build-all.ps1
```

The Gradle wrapper downloads everything it needs, including a **JDK 21 toolchain**
to compile against (via the Foojay resolver) — you don't need to install JDK 21
yourself even on a newer JDK. Per-version jars land in `build/libs/` (and
`build-all.ps1` collects them into `dist/`).

## Install

1. Install **Fabric Loader** (0.16+) for **your** Minecraft version.
2. Put these in `.minecraft/mods`:
   * the **TurboBoost jar that matches your MC version** (see the table above)
   * **Fabric API** — https://modrinth.com/mod/fabric-api
   * **Sodium** + **Lithium** *(strongly recommended — they do the heavy
     engine-level optimization TurboBoost intentionally doesn't reinvent)*
3. Launch. The overlay appears top-left and shows `LINK ✔` once the desktop app
   is open.

## Keybinds (rebindable in Options → Controls → TurboBoost)

| Key | Action |
|-----|--------|
| **F6** | Toggle the FPS/RAM overlay |
| **F7** | Cycle boost profile (Off → Quality → Balanced → Potato) |
| **F8** | Clean RAM (free heap, reports MB freed) |
| **F9** | Benchmark FPS (5s baseline → boost → 5s, shows the gain) |
| **F10** | Toggle Iris shaders on/off (instant FPS panic button) |
| **K**  | Quick-switch to the quietest server in your category |

Plus **Auto-Boost** (applies the boost when in-world FPS sits below
`autoBoostFpsThreshold`, default 30, for ~3s) and **Iris integration**: the
Balanced/Potato boost levels turn shaders off for FPS and back on at Off
(`boostDisablesShaders`). All Iris access is via reflection on its stable v0 API,
so it's a soft dependency — no Iris, no problem.

## Config (auto-created in `.minecraft/config/`)

* `turboboost.json` — overlay, dynamic-FPS, link host/port, and the
  `quickSwitchCategory` the **K** key targets (default `Casual/Beginner`).
* `turboboost-servers.json` — offline fallback server list (the app's list is
  authoritative when connected).

## How multi-version works (no mixins, no Stonecutter)

All shared code lives in `src/main/java`. The handful of APIs that differ between
MC versions are isolated behind the [`TbCompat`](src/main/java/com/megaultra/turboboost/compat/TbCompat.java)
interface, and each version supplies a `CompatImpl` in its own **source overlay**,
mixed into the build by the `MC_MATRIX` in [`build.gradle`](build.gradle):

| overlay | versions | differences |
|---------|----------|-------------|
| `src/v1_21_1` | 1.21.1 | `getGraphicsMode()`, String keybind category, `ParticlesMode` in `client.option` |
| `src/legacy`  | 1.21.4, 1.21.8 | `getGraphicsMode()`, String keybind category, `ParticlesMode` in `net.minecraft.particle` |
| `src/modern`  | 1.21.11 | `getPreset()`, `KeyBinding.Category`, `ParticlesMode` in `net.minecraft.particle` |

The HUD render callback (its 2nd arg changed from `float` to `RenderTickCounter`)
is handled with an untyped lambda in
[`FpsHudOverlay`](src/main/java/com/megaultra/turboboost/hud/FpsHudOverlay.java),
so it needs no overlay.

**To add another version:** add a row to `MC_MATRIX` (get the coordinates from
https://fabricmc.net/develop), point it at the closest overlay, run
`./gradlew build -Pmcver=<new>`, and fix any compile error by moving the offending
call into `TbCompat` + the overlays.
