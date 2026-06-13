# Publishing TurboBoost to Modrinth

Everything you need is ready in this folder:

| What | File |
|------|------|
| The mod jar to upload | `build/libs/turboboost-1.0.0.jar` |
| Project icon (512×512) | `modrinth-icon.png` |
| Description (paste into the page) | `MODRINTH_DESCRIPTION.md` |
| License | `LICENSE` (MIT) |

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
| **License** | `MIT` |
| **Environments** | Client: **Required** · Server: **Unsupported** (it's client-side) |
| **Categories / tags** | `Optimization`, `Utility`, `Management` |
| **Links** | leave Source/Issues blank for now (or add a GitHub repo later) |

Save the draft.

## Step 3 — Upload the version
Inside the project → **Versions** → **Create version**.

| Field | Value |
|-------|-------|
| **Version number** | `1.0.0` |
| **Version title** | `TurboBoost 1.0.0` |
| **Release channel** | Release |
| **Loaders** | `Fabric` |
| **Game versions** | `1.21.11` |
| **Primary file** | upload `build/libs/turboboost-1.0.0.jar` |
| **Changelog** | `First release: Boost profile, FPS/RAM overlay, dynamic FPS, smart server switcher, and the desktop app live-link.` |

**Dependencies** (add these on the version page):
- **Fabric API** → type **Required**
- **Sodium** → type **Optional**
- **Lithium** → type **Optional**

> Tip: the sources jar (`turboboost-1.0.0-sources.jar`) is optional — only upload
> it as an *additional* file if you want source available; it is **not** the file
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
- ✅ MIT license is set, and the `LICENSE` file is inside the jar.

## Updating later (optional, scriptable)
For future versions you don't need to click through the site — see
`publish-to-modrinth.ps1` in this folder, which uploads a new version via the
Modrinth API with a token. (The first publish is easiest via the website above.)
