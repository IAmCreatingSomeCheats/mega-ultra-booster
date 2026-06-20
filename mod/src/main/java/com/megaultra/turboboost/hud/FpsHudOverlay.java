/*
 * MEGA Ultra Booster
 * Copyright (c) 2026 IAmCreatingSomeCheats. All Rights Reserved.
 * Proprietary software — unauthorized use, copying, modification, or
 * distribution of this source code is prohibited. See LICENSE for terms.
 */

package com.megaultra.turboboost.hud;

import com.megaultra.turboboost.TurboBoostClient;
import com.megaultra.turboboost.config.BoostConfig;
import com.megaultra.turboboost.shader.IrisShaders;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Compact FPS / frame-time / RAM / link-status overlay drawn top-left.
 * The text + reflection are rebuilt ~5x/sec (not every frame) into a reused list,
 * so a 200+ fps game doesn't churn the GC building HUD strings each frame.
 */
public final class FpsHudOverlay {

    private static final List<String> LINES = new ArrayList<>(8);
    private static final long REBUILD_INTERVAL_MS = 200L;
    private static long lastBuildMs = 0L;

    private FpsHudOverlay() {}

    public static void register() {
        // Untyped lambda: the second arg is `float` (1.21.1) or `RenderTickCounter`
        // (1.21.2+). We ignore it, so Java infers the right type per MC version and
        // this compiles unchanged across all targets.
        HudRenderCallback.EVENT.register((ctx, tickDelta) -> render(ctx));
    }

    private static void render(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        BoostConfig cfg = TurboBoostClient.getConfig();
        if (cfg == null || !cfg.hudEnabled) return;
        if (client.options.hudHidden) return;        // respect F1
        if (client.textRenderer == null) return;

        long now = System.currentTimeMillis();
        if (now - lastBuildMs >= REBUILD_INTERVAL_MS || LINES.isEmpty()) {
            rebuild(client, cfg);
            lastBuildMs = now;
        }

        int x = cfg.hudX;
        int y = cfg.hudY;
        int lineH = client.textRenderer.fontHeight + 2;
        for (int i = 0; i < LINES.size(); i++) {
            ctx.drawTextWithShadow(client.textRenderer, LINES.get(i), x, y + i * lineH, 0xFFFFFF);
        }
    }

    /** Rebuild the cached HUD lines. Runs a few times a second, not per frame. */
    private static void rebuild(MinecraftClient client, BoostConfig cfg) {
        LINES.clear();
        int fps = client.getCurrentFps();
        LINES.add("§b⚡ TurboBoost");
        LINES.add(fpsColor(fps) + fps + " FPS §7(" + frameTime(fps) + " ms)");

        if (cfg.hudShowStats) {
            var stats = TurboBoostClient.getFpsStats();
            if (stats.hasData()) {
                LINES.add("§7avg §f" + stats.avg() + " §7· 1% low §f" + stats.onePercentLow());
            }
        }
        if (cfg.hudShowMemory) {
            Runtime rt = Runtime.getRuntime();
            long usedMb = (rt.totalMemory() - rt.freeMemory()) / 1_048_576L;
            long maxMb = rt.maxMemory() / 1_048_576L;
            int pct = maxMb > 0 ? (int) (usedMb * 100 / maxMb) : 0;
            LINES.add(memColor(pct) + "RAM " + usedMb + "/" + maxMb + " MB §7(" + pct + "%)");
        }
        if (cfg.hudShowServer) {
            LINES.add("§7" + serverLabel(client));
        }
        if (cfg.hudShowLinkStatus) {
            boolean linked = TurboBoostClient.getLink() != null && TurboBoostClient.getLink().isConnected();
            LINES.add(linked ? "§aLINK ✔" : "§8LINK ✖");
        }
        if (cfg.hudShowShaders && IrisShaders.isAvailable()) {
            LINES.add("§7Shaders " + (IrisShaders.shadersEnabled() ? "§aON" : "§8OFF"));
        }
    }

    private static String frameTime(int fps) {
        return fps <= 0 ? "—" : String.format("%.1f", 1000.0 / fps);
    }

    private static String fpsColor(int fps) {
        if (fps >= 90) return "§a";
        if (fps >= 45) return "§e";
        return "§c";
    }

    private static String memColor(int pct) {
        if (pct >= 90) return "§c";
        if (pct >= 70) return "§e";
        return "§a";
    }

    private static String serverLabel(MinecraftClient client) {
        ServerInfo si = client.getCurrentServerEntry();
        if (si != null && si.address != null) return si.address;
        if (client.isInSingleplayer()) return "Singleplayer";
        return "Main Menu";
    }
}
