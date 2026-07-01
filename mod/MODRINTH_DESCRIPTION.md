# TurboBoost

A client-side Fabric utility mod: switchable performance profiles, an FPS/RAM
overlay, dynamic FPS, a manual benchmark, optional Iris shader toggling, and a
server-list switcher. An optional desktop companion app can connect to it.

TurboBoost adjusts Minecraft's own video options for you and shows what the game
is doing. It does **not** replace [Sodium](https://modrinth.com/mod/sodium) or
[Lithium](https://modrinth.com/mod/lithium) — it does not change the rendering or
tick engine. For the largest FPS improvements, install Sodium + Lithium; TurboBoost
runs alongside them and handles the settings automation, overlay, and tools.

## Features

- **Boost profiles** — one key cycles Off → Quality → Balanced → Potato. Each
  profile sets vanilla video options (graphics mode, clouds, particles, entity
  shadows, vsync, render/entity distance). Your previous settings are restored on
  "Off".
- **FPS / RAM overlay** — shows FPS, frame time, average and 1% low FPS, JVM heap
  usage, current server, and companion-app link status. Toggle with a key.
- **Dynamic FPS** — lowers the framerate cap while the game window is unfocused or
  minimized, which can reduce GPU load and power use.
- **Auto-Boost** *(optional)* — applies the boost profile if in-game FPS stays
  below a configurable threshold for a few seconds.
- **FPS benchmark** — measures average FPS for 5s, applies the boost, measures 5s
  more, and reports the before/after numbers.
- **Server-list switcher** — keep your servers grouped by category; one key
  reconnects you to the one with the fewest players / lowest ping in a category
  you choose. (It cannot detect player skill — it sorts by live player count and
  ping only.)
- **Iris shader toggling** *(optional)* — if [Iris](https://modrinth.com/mod/iris)
  is installed, a key turns shaders on/off, and the heavier boost profiles turn
  shaders off (and back on at "Off"). Does nothing if Iris is absent.
- **Companion app link** *(optional)* — connects to the desktop app over a
  loopback-only socket (`127.0.0.1`) so the app can apply a profile and read
  FPS/RAM. The mod works fully without the app.

## Default keybinds (rebindable in Controls)

| Key | Action |
|-----|--------|
| **F6** | Toggle the FPS/RAM overlay |
| **F7** | Cycle boost profile (Off → Quality → Balanced → Potato) |
| **F9** | Run the before/after FPS benchmark |
| **F10** | Toggle Iris shaders (if Iris is installed) |
| **K**  | Switch to the lowest-population server in your category |

## Requirements

- **Minecraft 1.20.1, 1.20.4, 1.20.6, 1.21, 1.21.1, 1.21.4, 1.21.8, or 1.21.11**
  (Fabric) — download the file matching your version
- **[Fabric API](https://modrinth.com/mod/fabric-api)** — required
- **[Sodium](https://modrinth.com/mod/sodium)** + **[Lithium](https://modrinth.com/mod/lithium)** — recommended, not bundled

## Optional companion app

A separate Windows desktop app can adjust RAM/JVM arguments, apply Windows
power/priority settings, and live-ping your server list. It is not required and
is not included here; the mod is fully functional on its own.

---

*Client-side only. It changes your own video options and which server you connect
to — no server-side behavior and no gameplay automation.*
