/*
 * MEGA Ultra Booster
 * Copyright (c) 2026 IAmCreatingSomeCheats. All Rights Reserved.
 * Proprietary software — unauthorized use, copying, modification, or
 * distribution of this source code is prohibited. See LICENSE for terms.
 */

package com.megaultra.turboboost.perf;

import com.megaultra.turboboost.TurboBoostClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.particle.ParticlesMode;

/**
 * Applies / reverts a {@link BoostProfile} by driving the vanilla video options.
 * Keeping it to vanilla options means it stays compatible with Sodium/Lithium
 * (which we recommend installing for the real engine-level wins) instead of
 * fighting them with custom rendering mixins.
 */
public final class PerformanceManager {

    private static BoostProfile snapshot; // pre-boost state, for revert / benchmark

    private PerformanceManager() {}

    public static void applyProfile(BoostProfile p) {
        MinecraftClient client = MinecraftClient.getInstance();
        GameOptions o = client.options;
        if (o == null) return;

        if (snapshot == null) {
            snapshot = capture(o);
        }

        if (p.renderDistance > 0) {
            o.getViewDistance().setValue(clamp(p.renderDistance, 2, 32));
        }
        o.getMaxFps().setValue(p.maxFps <= 0 ? 260 : clamp(p.maxFps, 10, 260));
        o.getPreset().setValue(p.graphicsFast ? GraphicsMode.FAST : GraphicsMode.FANCY);
        o.getCloudRenderMode().setValue(p.cloudsOff ? CloudRenderMode.OFF : CloudRenderMode.FANCY);
        o.getParticles().setValue(parseParticles(p.particles));
        o.getEntityShadows().setValue(p.entityShadows);
        o.getEnableVsync().setValue(p.vsync);
        o.getEntityDistanceScaling().setValue(clampD(p.entityDistance, 0.5, 1.0));

        // Persist + let dynamic FPS use the new flag immediately.
        TurboBoostClient.getConfig().dynamicFps = p.dynamicFps;
        o.write();
        TurboBoostClient.LOGGER.info("[TurboBoost] Applied boost profile (rd={}, maxFps={})",
                p.renderDistance, p.maxFps);
    }

    /** Restore the options captured before the first boost of this session. */
    public static void revert() {
        if (snapshot == null) return;
        applyRaw(snapshot);
        snapshot = null;
    }

    private static void applyRaw(BoostProfile p) {
        MinecraftClient client = MinecraftClient.getInstance();
        GameOptions o = client.options;
        if (o == null) return;
        o.getViewDistance().setValue(clamp(p.renderDistance, 2, 32));
        o.getMaxFps().setValue(p.maxFps <= 0 ? 260 : p.maxFps);
        o.getPreset().setValue(p.graphicsFast ? GraphicsMode.FAST : GraphicsMode.FANCY);
        o.getCloudRenderMode().setValue(p.cloudsOff ? CloudRenderMode.OFF : CloudRenderMode.FANCY);
        o.getParticles().setValue(parseParticles(p.particles));
        o.getEntityShadows().setValue(p.entityShadows);
        o.getEnableVsync().setValue(p.vsync);
        o.getEntityDistanceScaling().setValue(clampD(p.entityDistance, 0.5, 1.0));
        o.write();
    }

    private static BoostProfile capture(GameOptions o) {
        BoostProfile s = new BoostProfile();
        s.renderDistance = o.getViewDistance().getValue();
        s.maxFps = o.getMaxFps().getValue();
        s.graphicsFast = o.getPreset().getValue() == GraphicsMode.FAST;
        s.cloudsOff = o.getCloudRenderMode().getValue() == CloudRenderMode.OFF;
        s.particles = switch (o.getParticles().getValue()) {
            case ALL -> "all";
            case DECREASED -> "decreased";
            case MINIMAL -> "minimal";
        };
        s.entityShadows = o.getEntityShadows().getValue();
        s.vsync = o.getEnableVsync().getValue();
        s.entityDistance = o.getEntityDistanceScaling().getValue();
        return s;
    }

    private static ParticlesMode parseParticles(String s) {
        if (s == null) return ParticlesMode.MINIMAL;
        return switch (s.toLowerCase()) {
            case "all" -> ParticlesMode.ALL;
            case "decreased" -> ParticlesMode.DECREASED;
            default -> ParticlesMode.MINIMAL;
        };
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static double clampD(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
