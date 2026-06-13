/*
 * MEGA Ultra Booster
 * Copyright (c) 2026 IAmCreatingSomeCheats. All Rights Reserved.
 * Proprietary software — unauthorized use, copying, modification, or
 * distribution of this source code is prohibited. See LICENSE for terms.
 */

package com.megaultra.turboboost.compat;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.SimpleOption;

/**
 * The few Minecraft APIs that differ between the versions TurboBoost targets.
 * Each supported MC version supplies a {@code CompatImpl} (in the per-version
 * {@code src/legacy} or {@code src/modern} source overlay) that the Gradle build
 * mixes into the main source set. Everything else in the mod is version-agnostic.
 *
 * Divergences handled here:
 *  - GameOptions graphics getter:  getGraphicsMode() (≤1.21.8) vs getPreset()         (1.21.11)
 *  - keybind category:             String key        (≤1.21.8) vs KeyBinding.Category (1.21.11)
 *  - ParticlesMode package:        client.option     (1.21.1)  vs net.minecraft.particle (1.21.4+)
 */
public interface TbCompat {

    /** The "Graphics" video option (Fast/Fancy/Fabulous). */
    SimpleOption<GraphicsMode> graphicsOption(GameOptions options);

    /** Create a keybind in TurboBoost's category for the given GLFW key code. */
    KeyBinding createKeyBind(String translationKey, int glfwKey);

    /** Set the particle option from a name: {@code "all"} | {@code "decreased"} | {@code "minimal"}. */
    void applyParticles(GameOptions options, String mode);

    /** Read the current particle option as {@code "all"} | {@code "decreased"} | {@code "minimal"}. */
    String readParticles(GameOptions options);
}
