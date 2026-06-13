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
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.util.InputUtil;

/**
 * Compat for Minecraft 1.21.1.
 * Like legacy (String keybind categories, getGraphicsMode()), but ParticlesMode
 * still lives in net.minecraft.client.option here (it moved to
 * net.minecraft.particle in 1.21.4).
 */
public final class CompatImpl implements TbCompat {

    @Override
    public SimpleOption<GraphicsMode> graphicsOption(GameOptions options) {
        return options.getGraphicsMode();
    }

    @Override
    public KeyBinding createKeyBind(String translationKey, int glfwKey) {
        return new KeyBinding(translationKey, InputUtil.Type.KEYSYM, glfwKey, "category.turboboost");
    }

    @Override
    public void applyParticles(GameOptions options, String mode) {
        ParticlesMode pm = switch (mode == null ? "minimal" : mode.toLowerCase()) {
            case "all" -> ParticlesMode.ALL;
            case "decreased" -> ParticlesMode.DECREASED;
            default -> ParticlesMode.MINIMAL;
        };
        options.getParticles().setValue(pm);
    }

    @Override
    public String readParticles(GameOptions options) {
        return switch (options.getParticles().getValue()) {
            case ALL -> "all";
            case DECREASED -> "decreased";
            case MINIMAL -> "minimal";
        };
    }
}
