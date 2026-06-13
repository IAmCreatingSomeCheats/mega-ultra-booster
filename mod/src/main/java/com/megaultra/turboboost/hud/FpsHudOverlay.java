/*
 * MEGA Ultra Booster
 * Copyright (c) 2026 IAmCreatingSomeCheats. All Rights Reserved.
 * Proprietary software — unauthorized use, copying, modification, or
 * distribution of this source code is prohibited. See LICENSE for terms.
 */

package com.megaultra.turboboost.hud;

import com.megaultra.turboboost.TurboBoostClient;
import com.megaultra.turboboost.config.BoostConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.List;

/** Compact FPS / frame-time / RAM / link-status overlay drawn top-left. */
public final class FpsHudOverlay {

    private FpsHudOverlay() {}

    public static void register() {
        HudRenderCallback.EVENT.register((DrawContext ctx, RenderTickCounter tick) -> render(ctx));
    }

    private static void render(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        BoostConfig cfg = TurboBoostClient.getConfig();
        if (cfg == null || !cfg.hudEnabled) return;
        if (client.options.hudHidden) return;        // respect F1
        if (client.textRenderer == null) return;

        List<String> lines = new ArrayList<>();
        int fps = client.getCurrentFps();
        lines.add("§b⚡ TurboBoost");
        lines.add(fpsColor(fps) + fps + " FPS §7(" + frameTime(fps) + " ms)");

        if (cfg.hudShowMemory) {
            Runtime rt = Runtime.getRuntime();
            long usedMb = (rt.totalMemory() - rt.freeMemory()) / 1_048_576L;
            long maxMb = rt.maxMemory() / 1_048_576L;
            int pct = maxMb > 0 ? (int) (usedMb * 100 / maxMb) : 0;
            lines.add(memColor(pct) + "RAM " + usedMb + "/" + maxMb + " MB §7(" + pct + "%)");
        }
        if (cfg.hudShowServer) {
            lines.add("§7" + serverLabel(client));
        }
        if (cfg.hudShowLinkStatus) {
            boolean linked = TurboBoostClient.getLink() != null && TurboBoostClient.getLink().isConnected();
            lines.add(linked ? "§aLINK ✔" : "§8LINK ✖");
        }

        int x = cfg.hudX;
        int y = cfg.hudY;
        int lineH = client.textRenderer.fontHeight + 2;
        for (int i = 0; i < lines.size(); i++) {
            ctx.drawTextWithShadow(client.textRenderer, lines.get(i), x, y + i * lineH, 0xFFFFFF);
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
