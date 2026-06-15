/*
 * MEGA Ultra Booster
 * Copyright (c) 2026 IAmCreatingSomeCheats. All Rights Reserved.
 * Proprietary software — unauthorized use, copying, modification, or
 * distribution of this source code is prohibited. See LICENSE for terms.
 */

package com.megaultra.turboboost.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.megaultra.turboboost.TurboBoostClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * User-tweakable settings, persisted to {@code config/turboboost.json}.
 * Plain public fields keep Gson (de)serialization trivial.
 */
public class BoostConfig {

    // ── FPS / RAM overlay ──
    public boolean hudEnabled = true;
    public int hudX = 4;
    public int hudY = 4;
    public boolean hudShowMemory = true;
    public boolean hudShowServer = true;
    public boolean hudShowLinkStatus = true;
    public boolean hudShowStats = true;     // average + 1% low
    public boolean hudShowShaders = true;   // Iris shader on/off (if installed)

    // ── Dynamic FPS (throttle while the window is unfocused) ──
    public boolean dynamicFps = true;
    public int unfocusedFps = 10;

    // ── Auto-Boost (apply the boost profile when FPS stays low) ──
    public boolean autoBoostEnabled = true;
    public int autoBoostFpsThreshold = 30;  // trigger when FPS sits below this

    // ── Shaders (Iris integration) ──
    // Turn shaders off on the heavier boost levels (Balanced/Potato) for FPS,
    // and back on when boost returns to Off. No-op if Iris isn't installed.
    public boolean boostDisablesShaders = true;

    // ── Live link to the desktop app ──
    public boolean linkEnabled = true;
    public String linkHost = "127.0.0.1";
    public int linkPort = 38910;

    // ── Smart server switcher ──
    /** Which category the in-game quick-switch key targets. */
    public String quickSwitchCategory = "Casual/Beginner";

    // ──────────────────────────────────────────────────────────────────────
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("turboboost.json");
    }

    public static BoostConfig load() {
        Path p = path();
        try {
            if (Files.exists(p)) {
                BoostConfig loaded = GSON.fromJson(Files.readString(p), BoostConfig.class);
                if (loaded != null) {
                    return loaded;
                }
            }
        } catch (Exception e) {
            TurboBoostClient.LOGGER.warn("[TurboBoost] Could not read config, using defaults", e);
        }
        BoostConfig fresh = new BoostConfig();
        fresh.save();
        return fresh;
    }

    public void save() {
        try {
            Files.writeString(path(), GSON.toJson(this));
        } catch (IOException e) {
            TurboBoostClient.LOGGER.warn("[TurboBoost] Could not save config", e);
        }
    }
}
