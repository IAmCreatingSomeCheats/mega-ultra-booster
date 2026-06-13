# Publishing TurboBoost to Modrinth

Everything you need is ready in this folder:

| What | File |
|------|------|
| The mod jars to upload (one per MC version) | `dist/turboboost-1.0.0+mc<version>.jar` (run `./build-all.ps1`) |
| Project icon (512×512) | `modrinth-icon.png` |
| Description (paste into the page) | `MODRINTH_DESCRIPTION.md` |
| License | `LICENSE` (All Rights Reserved) |

The slug **`turboboost`** was free when checked — claim it before someone else does.

---

## Step 1 — Account
1. Go to <https://modrinth.com> → **Sign Up** (GitHub / GitLab / Google / email).
2. Verify your email.

## Step 2 — Create the project
Top-right avatar → **Creator dashboard** → **Projects** → **Create a project**.

| Field | Value |
|-------|-------|
| **Project type** | Mod |
| **Name** | `TurboBoost` |
| **Slug / URL** | `turboboost` |
| **Summary** | `One-key FPS profiles, a live FPS/RAM overlay, dynamic FPS, and a smart server switcher — with an optional desktop companion app. Pairs with Sodium + Lithium.` |
| **Icon** | upload `modrinth-icon.png` |
| **Description** | paste the full contents of `MODRINTH_DESCRIPTION.md` |
| **License** | `All Rights Reserved` (ARR) |
| **Environments** | Client: **Required** · Server: **Unsupported** (it's client-side) |
| **Categories / tags** | `Optimization`, `Utility`, `Management` |
| **Links** | leave Source/Issues blank for now (or add a GitHub repo later) |

Save the draft.

## Step 3 — Upload the versions
TurboBoost ships a **separate jar per Minecraft version** (mappings differ), so
each MC version is its own Modrinth "version". Inside the project →
**Versions** → **Create version**, and repeat for each jar in `dist/`:

| Field | Value |
|-------|-------|
| **Version number** | `1.0.0+mc<version>` (e.g. `1.0.0+mc1.21.8`) — must be unique |
| **Version title** | `TurboBoost 1.0.0 (MC <version>)` |
| **Release channel** | Release |
| **Loaders** | `Fabric` |
| **Game versions** | the matching version only (e.g. `1.21.8`) |
| **Primary file** | the matching `dist/turboboost-1.0.0+mc<version>.jar` |
| **Changelog** | `First release: Boost profile, FPS/RAM overlay, dynamic FPS, smart server switcher, and the desktop app live-link.` |

**Dependencies** (add to each version):
- **Fabric API** → type **Required**
- **Sodium** → type **Optional**
- **Lithium** → type **Optional**

> ⚡ **Faster:** instead of doing all four by hand, run `./build-all.ps1` then
> `./publish-to-modrinth.ps1` — it uploads every jar in `dist/` as its own version
> with the right game version + dependencies. (The very first publish still needs
> the project created via Steps 1–2 on the website.)

> Tip: don't upload the `-sources.jar` files — those are source, not the mod
> people install.

## Step 4 — Submit for review
New projects are hidden until a moderator approves them. Click **Submit for
review** (you need the icon, description, license, and one version uploaded — all
set). Approval is usually quick.

---

## A few rules worth knowing (so you don't get rejected)
- ✅ Keep the description honest — TurboBoost is an optimization/utility mod and a
  server switcher. That's all allowed.
- ❌ Don't describe it as a "hack/cheat" or as giving an unfair multiplayer
  advantage — it doesn't, so don't imply it.
- ✅ You're not bundling Sodium/Lithium/Fabric API — good (don't reupload other
  people's mods).
- ✅ All Rights Reserved license is set, and the `LICENSE` file is inside the jar.

## Updating later (scriptable)
You don't need to click through the site for updates:

```powershell
$env:MODRINTH_TOKEN = "mrp_xxx"   # from https://modrinth.com/settings/pats
./build-all.ps1                    # builds every version into dist/
./publish-to-modrinth.ps1          # uploads each as its own Modrinth version
```

(The first publish still needs the project itself created via Steps 1–2.)
