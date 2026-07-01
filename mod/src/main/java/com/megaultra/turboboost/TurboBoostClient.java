/*
 * MEGA Ultra Booster
 * Copyright (c) 2026 IAmCreatingSomeCheats. All Rights Reserved.
 * Proprietary software — unauthorized use, copying, modification, or
 * distribution of this source code is prohibited. See LICENSE for terms.
 */

package com.megaultra.turboboost;

import com.megaultra.turboboost.compat.Compat;
import com.megaultra.turboboost.config.BoostConfig;
import com.megaultra.turboboost.hud.FpsHudOverlay;
import com.megaultra.turboboost.link.AppLinkClient;
import com.megaultra.turboboost.perf.BoostProfile;
import com.megaultra.turboboost.perf.DynamicFps;
import com.megaultra.turboboost.perf.FpsStats;
import com.megaultra.turboboost.perf.PerformanceManager;
import com.megaultra.turboboost.server.ServerEntry;
import com.megaultra.turboboost.server.ServerStore;
import com.megaultra.turboboost.server.ServerSwitcher;
import com.megaultra.turboboost.shader.IrisShaders;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TurboBoost client entrypoint. Wires up the overlay, keybinds, performance
 * profiles, smart server switcher, and the live link to the desktop app.
 */
public class TurboBoostClient implements ClientModInitializer {
    public static final String MOD_ID = "turboboost";
    public static final String MOD_VERSION = "1.3.2";
    public static final Logger LOGGER = LoggerFactory.getLogger("TurboBoost");

    private static BoostConfig config;
    private static AppLinkClient link;
    private static ServerStore serverStore;
    private static final FpsStats fpsStats = new FpsStats();

    private KeyBinding toggleHudKey;
    private KeyBinding boostCycleKey;
    private KeyBinding quickSwitchKey;
    private KeyBinding benchmarkKey;
    private KeyBinding shaderToggleKey;
    private final DynamicFps dynamicFps = new DynamicFps();
    private int telemetryTimer = 0;
    private int lowFpsTicks = 0;       // sustained-low counter for Auto-Boost
    private int worldTicks = 0;        // ticks since the world+player loaded (grace period)
    private boolean autoBoosted = false;
    private int boostLevel = 0;        // 0 Off · 1 Quality · 2 Balanced · 3 Potato
    private boolean shadersDisabledByBoost = false;

    // Benchmark state machine (0 idle · 1 baseline · 2 boosted)
    private int benchState = 0;
    private int benchTicks = 0;
    private long benchSum = 0;
    private int benchCount = 0;
    private int benchBefore = 0;

    public static BoostConfig getConfig() {
        return config;
    }

    public static FpsStats getFpsStats() {
        return fpsStats;
    }

    public static AppLinkClient getLink() {
        return link;
    }

    public static ServerStore getServerStore() {
        return serverStore;
    }

    @Override
    public void onInitializeClient() {
        config = BoostConfig.load();
        serverStore = new ServerStore();
        serverStore.load();

        registerKeybinds();
        FpsHudOverlay.register();

        link = new AppLinkClient(config);
        link.start();

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> {
            if (link != null) link.stop();
        });

        LOGGER.info("[TurboBoost] v{} ready. Tip: install Sodium + Lithium for the biggest FPS gains.", MOD_VERSION);
    }

    private void registerKeybinds() {
        toggleHudKey = KeyBindingHelper.registerKeyBinding(
                Compat.get().createKeyBind("key.turboboost.toggle_hud", GLFW.GLFW_KEY_F6));
        boostCycleKey = KeyBindingHelper.registerKeyBinding(
                Compat.get().createKeyBind("key.turboboost.boost_cycle", GLFW.GLFW_KEY_F7));
        quickSwitchKey = KeyBindingHelper.registerKeyBinding(
                Compat.get().createKeyBind("key.turboboost.quick_switch", GLFW.GLFW_KEY_K));
        benchmarkKey = KeyBindingHelper.registerKeyBinding(
                Compat.get().createKeyBind("key.turboboost.benchmark", GLFW.GLFW_KEY_F9));
        shaderToggleKey = KeyBindingHelper.registerKeyBinding(
                Compat.get().createKeyBind("key.turboboost.shader_toggle", GLFW.GLFW_KEY_F10));
    }

    private void onClientTick(MinecraftClient client) {
        while (toggleHudKey.wasPressed()) {
            config.hudEnabled = !config.hudEnabled;
            config.save();
            actionBar(config.hudEnabled ? "§a⚡ Overlay ON" : "§7⚡ Overlay OFF");
        }
        while (boostCycleKey.wasPressed()) {
            cycleBoost();
        }
        while (quickSwitchKey.wasPressed()) {
            quickSwitch();
        }
        while (benchmarkKey.wasPressed()) {
            startBenchmark(client);
        }
        while (shaderToggleKey.wasPressed()) {
            toggleShaders();
        }

        dynamicFps.tick(client, config);
        updateStatsAndAutoBoost(client);
        tickBenchmark(client);

        if (link != null && link.isConnected() && client.world != null) {
            if (++telemetryTimer >= 20) { // ~once per second
                telemetryTimer = 0;
                link.sendTelemetry();
            }
        }
    }

    /** Feed the FPS window and trigger Auto-Boost once if FPS stays low while playing. */
    private void updateStatsAndAutoBoost(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            // Out of a world (or still loading): reset so Auto-Boost starts fresh.
            worldTicks = 0;
            lowFpsTicks = 0;
            autoBoosted = false;
            return;
        }

        int fps = client.getCurrentFps();
        fpsStats.record(fps);

        // Grace period: skip the FPS dip during world load/gen (~10 s) so Auto-Boost
        // doesn't false-fire on the loading spike and slash render distance on join.
        if (worldTicks < 200) {
            worldTicks++;
            return;
        }

        if (!config.autoBoostEnabled || autoBoosted) return;

        if (fps > 0 && fps < config.autoBoostFpsThreshold) {
            if (++lowFpsTicks >= 100) { // ~5 s of genuinely low FPS while playing
                PerformanceManager.applyProfile(BoostProfile.balanced());
                boostLevel = 2;
                autoBoosted = true;
                lowFpsTicks = 0;
                actionBar("§e⚡ Auto-Boost: low FPS — applied Balanced (press F7 to change/undo)");
            }
        } else {
            lowFpsTicks = 0;
        }
    }

    private void quickSwitch() {
        String category = config.quickSwitchCategory;
        if (link != null && link.isConnected()) {
            // Ask the app to pick the quietest server in this category; it replies with switch_server.
            link.requestEasyServer(category);
            actionBar("§b⚡ Finding the quietest §f" + category + " §bserver…");
        } else {
            // Offline fallback: cycle through the local list.
            ServerEntry next = serverStore.nextInCategory(category);
            if (next != null) {
                ServerSwitcher.connect(next.name, next.address);
            } else {
                actionBar("§cNo servers tagged '" + category + "'. Add some in the app.");
            }
        }
    }

    /** F7: cycle Off → Quality → Balanced → Potato. */
    private void cycleBoost() {
        boostLevel = (boostLevel + 1) % 4;
        switch (boostLevel) {
            case 1 -> { PerformanceManager.applyProfile(BoostProfile.quality());  actionBar("§a⚡ Boost: §fQuality §7(light)"); }
            case 2 -> { PerformanceManager.applyProfile(BoostProfile.balanced()); actionBar("§a⚡ Boost: §fBalanced"); }
            case 3 -> { PerformanceManager.applyProfile(BoostProfile.potato());   actionBar("§a⚡ Boost: §fPotato §7(max FPS)"); }
            default -> { PerformanceManager.revert();                             actionBar("§7⚡ Boost: §fOff §7(settings restored)"); }
        }
        applyShadersForBoostLevel();
    }

    /** Drop shaders on the heavier boost levels (and restore them on lighter/off). */
    private void applyShadersForBoostLevel() {
        if (!config.boostDisablesShaders || !IrisShaders.isAvailable()) return;
        boolean wantOff = (boostLevel == 2 || boostLevel == 3); // Balanced / Potato
        if (wantOff && IrisShaders.shadersEnabled()) {
            IrisShaders.setShaders(false);
            shadersDisabledByBoost = true;
            actionBar("§7⚡ Shaders off for FPS — F10 to bring them back");
        } else if (!wantOff && shadersDisabledByBoost) {
            IrisShaders.setShaders(true);
            shadersDisabledByBoost = false;
        }
    }

    /** F10: manually toggle Iris shaders (panic FPS button). */
    private void toggleShaders() {
        if (!IrisShaders.isAvailable()) {
            actionBar("§c⚡ Iris not installed — no shaders to toggle");
            return;
        }
        boolean wasOn = IrisShaders.shadersEnabled();
        IrisShaders.setShaders(!wasOn);
        shadersDisabledByBoost = false; // manual override wins
        actionBar(wasOn ? "§7⚡ Shaders §fOFF §7(max FPS)" : "§a⚡ Shaders §fON");
    }

    /** F9: measure avg FPS for 5s, apply the boost, measure 5s more, show the gain. */
    private void startBenchmark(MinecraftClient client) {
        if (benchState != 0) { actionBar("§e⚡ Benchmark already running…"); return; }
        if (client.world == null) { actionBar("§c⚡ Benchmark: join a world first"); return; }
        benchState = 1;
        benchTicks = 100; // ~5 s at 20 tps
        benchSum = 0;
        benchCount = 0;
        actionBar("§b⚡ Benchmark: measuring baseline (5s)…");
    }

    private void tickBenchmark(MinecraftClient client) {
        if (benchState == 0) return;
        if (client.world == null) { benchState = 0; return; } // aborted on disconnect

        benchSum += client.getCurrentFps();
        benchCount++;
        if (--benchTicks > 0) return;

        int avg = benchCount > 0 ? (int) (benchSum / benchCount) : 0;
        if (benchState == 1) {
            benchBefore = avg;
            PerformanceManager.applyProfile(BoostProfile.potato());
            boostLevel = 3;
            benchSum = 0;
            benchCount = 0;
            benchTicks = 100;
            benchState = 2;
            actionBar("§b⚡ Boost applied — measuring boosted (5s)…");
        } else {
            benchState = 0;
            int delta = avg - benchBefore;
            int pct = benchBefore > 0 ? delta * 100 / benchBefore : 0;
            String sign = delta >= 0 ? "+" : "";
            actionBar("§a⚡ Benchmark: §f" + benchBefore + " → " + avg + " FPS §7(" + sign + delta + ", " + sign + pct + "%)");
        }
    }

    /** Show a short message on the action bar (legacy §-codes supported). */
    public static void actionBar(String legacyText) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal(legacyText), true);
        } else if (client.inGameHud != null) {
            client.inGameHud.setOverlayMessage(Text.literal(legacyText), false);
        }
    }
}
