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
    public static final String MOD_VERSION = "1.1.0";
    public static final Logger LOGGER = LoggerFactory.getLogger("TurboBoost");

    private static BoostConfig config;
    private static AppLinkClient link;
    private static ServerStore serverStore;
    private static final FpsStats fpsStats = new FpsStats();

    private KeyBinding toggleHudKey;
    private KeyBinding boostNowKey;
    private KeyBinding quickSwitchKey;
    private final DynamicFps dynamicFps = new DynamicFps();
    private int telemetryTimer = 0;
    private int lowFpsTicks = 0;       // sustained-low counter for Auto-Boost
    private boolean autoBoosted = false;

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
        boostNowKey = KeyBindingHelper.registerKeyBinding(
                Compat.get().createKeyBind("key.turboboost.boost_now", GLFW.GLFW_KEY_F7));
        quickSwitchKey = KeyBindingHelper.registerKeyBinding(
                Compat.get().createKeyBind("key.turboboost.quick_switch", GLFW.GLFW_KEY_K));
    }

    private void onClientTick(MinecraftClient client) {
        while (toggleHudKey.wasPressed()) {
            config.hudEnabled = !config.hudEnabled;
            config.save();
            actionBar(config.hudEnabled ? "§a⚡ Overlay ON" : "§7⚡ Overlay OFF");
        }
        while (boostNowKey.wasPressed()) {
            PerformanceManager.applyProfile(BoostProfile.aggressive());
            actionBar("§a⚡ Boost profile applied");
        }
        while (quickSwitchKey.wasPressed()) {
            quickSwitch();
        }

        dynamicFps.tick(client, config);
        updateStatsAndAutoBoost(client);

        if (link != null && link.isConnected() && client.world != null) {
            if (++telemetryTimer >= 20) { // ~once per second
                telemetryTimer = 0;
                link.sendTelemetry();
            }
        }
    }

    /** Feed the FPS window and trigger Auto-Boost once if FPS stays low in-world. */
    private void updateStatsAndAutoBoost(MinecraftClient client) {
        if (client.world == null) {
            // Out of a world: reset so Auto-Boost can fire again next session.
            lowFpsTicks = 0;
            autoBoosted = false;
            return;
        }

        int fps = client.getCurrentFps();
        fpsStats.record(fps);

        if (!config.autoBoostEnabled || autoBoosted) return;

        if (fps > 0 && fps < config.autoBoostFpsThreshold) {
            if (++lowFpsTicks >= 60) { // ~3 s sustained below the threshold
                PerformanceManager.applyProfile(BoostProfile.aggressive());
                autoBoosted = true;
                lowFpsTicks = 0;
                actionBar("§e⚡ Auto-Boost: FPS was low — applied boost profile");
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
