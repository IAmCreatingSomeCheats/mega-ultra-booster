# MEGA Ultra Booster — Live Link Protocol

The desktop **app** and the in-game **mod** talk to each other over a plain
**TCP socket on `127.0.0.1:38910`** (loopback only — never exposed to the network).

The wire format is **newline-delimited JSON** (one JSON object per line, `\n`
terminated, UTF-8). No external libraries are required on either side:

* App side: `System.Text.Json` + `TcpListener`.
* Mod side: Minecraft already bundles **Gson** + `java.net.Socket`.

The **app is the server**, the **mod is the client**. The mod reconnects
automatically with backoff whenever the app isn't running yet.

```
┌────────────────────────┐        TCP 127.0.0.1:38910        ┌────────────────────┐
│  MEGA Ultra Booster app │◄────── newline JSON ─────────────►│  TurboBoost mod    │
│  (LinkServer, listener) │                                   │  (AppLinkClient)   │
└────────────────────────┘                                   └────────────────────┘
```

## Message envelope

Every message is a single JSON object on one line:

```json
{ "type": "<string>", "data": { ... } }
```

Unknown `type` values must be ignored (forward-compatible).

---

## Mod → App

| type                  | data                                                                 | meaning |
|-----------------------|----------------------------------------------------------------------|---------|
| `hello`               | `{ "mcVersion": "1.21.11", "modVersion": "1.0.0" }`                   | sent on connect |
| `telemetry`           | `{ "fps": 142, "frameTimeMs": 7.0, "memUsedMb": 1840, "memMaxMb": 4096, "dimension": "minecraft:overworld", "server": "mc.hypixel.net" }` | ~1×/sec |
| `request_easy_server` | `{ "category": "Casual/Beginner" }`                                   | user pressed the in-game "quick switch" key; app replies with `switch_server` |
| `boost_state`         | `{ "active": true }`                                                  | echo after applying a profile |

## App → Mod

| type            | data                                                                                                       | meaning |
|-----------------|------------------------------------------------------------------------------------------------------------|---------|
| `apply_profile` | `{ "renderDistance": 8, "maxFps": 0, "graphicsFast": true, "cloudsOff": true, "particles": "minimal", "entityShadows": false, "vsync": false, "entityDistance": 0.5, "dynamicFps": true }` | the BOOST button — tune the live game |
| `switch_server` | `{ "name": "Quiet SMP", "address": "play.example.net" }`                                                    | disconnect and connect to this server |
| `set_hud`       | `{ "enabled": true }`                                                                                       | toggle the FPS/RAM overlay |
| `ping`          | `{}`                                                                                                        | keepalive; mod ignores or may reply |

### `apply_profile` field notes
* `maxFps`: `0` means **unlimited** (vanilla treats 260 as unlimited; the mod maps `0 → 260`).
* `particles`: one of `all` \| `decreased` \| `minimal`.
* `entityDistance`: `0.5`–`1.0` (vanilla "Entity Distance" scaling).
* `dynamicFps`: when true, the mod throttles FPS while the window is unfocused.

---

## "Find an easier server" flow

This is the feasible, honest version of *"send me somewhere with newer/quieter players."*
Nothing can read the actual skill of players on an arbitrary server, so the app
ranks **your own tagged servers** by live population + ping instead.

```
in-game key (default K)
        │  request_easy_server { category: "Casual/Beginner" }
        ▼
   app pings every server tagged "Casual/Beginner"  (Server List Ping)
        │  picks lowest playerCount, tie-break lowest ping
        ▼
   switch_server { name, address }
        │
        ▼
   mod disconnects and connects you there
```
