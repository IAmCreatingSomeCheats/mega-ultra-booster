/*
 * MEGA Ultra Booster
 * Copyright (c) 2026 IAmCreatingSomeCheats. All Rights Reserved.
 * Proprietary software — unauthorized use, copying, modification, or
 * distribution of this source code is prohibited. See LICENSE for terms.
 */

package com.megaultra.turboboost.perf;

import com.megaultra.turboboost.config.BoostConfig;
import net.minecraft.client.MinecraftClient;

/**
 * Dynamic FPS without any mixin: while the game window is unfocused, temporarily
 * lower the vanilla {@code maxFps} option so the render loop idles; restore it on
 * refocus. Driven once per client tick. Version-stable across MC updates because
 * it only touches the public options API.
 */
public final class DynamicFps {

    private boolean throttled = false;
    private int savedMaxFps = 260;

    public void tick(MinecraftClient client, BoostConfig config) {
        if (client.options == null) return;

        boolean shouldThrottle = config.dynamicFps && !client.isWindowFocused();

        if (shouldThrottle && !throttled) {
            savedMaxFps = client.options.getMaxFps().getValue();
            client.options.getMaxFps().setValue(Math.max(1, config.unfocusedFps));
            throttled = true;
        } else if (!shouldThrottle && throttled) {
            client.options.getMaxFps().setValue(savedMaxFps);
            throttled = false;
        }
    }
}
