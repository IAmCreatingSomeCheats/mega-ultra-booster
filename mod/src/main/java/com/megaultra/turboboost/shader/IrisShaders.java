/*
 * MEGA Ultra Booster
 * Copyright (c) 2026 IAmCreatingSomeCheats. All Rights Reserved.
 * Proprietary software — unauthorized use, copying, modification, or
 * distribution of this source code is prohibited. See LICENSE for terms.
 */

package com.megaultra.turboboost.shader;

import java.lang.reflect.Method;

/**
 * Optional integration with the <b>Iris</b> shaders mod, accessed entirely through
 * reflection on its stable v0 API (<code>net.irisshaders.iris.api.v0</code>). This
 * means TurboBoost needs no compile-time Iris dependency, works on every MC version,
 * and simply no-ops when Iris isn't installed.
 *
 * Used to toggle shaders off for instant FPS (boost / panic key) and back on.
 */
public final class IrisShaders {

    private static boolean resolved = false;
    private static boolean available = false;

    private static Object api;                    // IrisApi singleton
    private static Method mIsShaderPackInUse;     // IrisApi#isShaderPackInUse()
    private static Method mGetConfig;             // IrisApi#getConfig()
    private static Method mAreShadersEnabled;     // IrisApiConfig#areShadersEnabled()
    private static Method mSetShadersEnabledAndApply; // IrisApiConfig#setShadersEnabledAndApply(boolean)

    private IrisShaders() {}

    private static synchronized void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            api = apiClass.getMethod("getInstance").invoke(null);
            mIsShaderPackInUse = apiClass.getMethod("isShaderPackInUse");
            mGetConfig = apiClass.getMethod("getConfig");

            // Resolve config methods from the public interface, not the impl class.
            Class<?> cfgInterface = Class.forName("net.irisshaders.iris.api.v0.IrisApiConfig");
            mAreShadersEnabled = cfgInterface.getMethod("areShadersEnabled");
            mSetShadersEnabledAndApply = cfgInterface.getMethod("setShadersEnabledAndApply", boolean.class);

            available = api != null;
        } catch (Throwable t) {
            available = false; // Iris not present (or API changed) — feature stays off.
        }
    }

    /** True if Iris is installed and its v0 API resolved. */
    public static boolean isAvailable() {
        resolve();
        return available;
    }

    /** Whether a shader pack is currently loaded/in use. */
    public static boolean shaderPackInUse() {
        if (!isAvailable()) return false;
        try {
            return (boolean) mIsShaderPackInUse.invoke(api);
        } catch (Throwable t) {
            return false;
        }
    }

    /** Whether shaders are currently enabled. */
    public static boolean shadersEnabled() {
        if (!isAvailable()) return false;
        try {
            Object config = mGetConfig.invoke(api);
            return (boolean) mAreShadersEnabled.invoke(config);
        } catch (Throwable t) {
            return false;
        }
    }

    /** Enable/disable shaders and apply (reloads the pack). No-op without Iris. */
    public static void setShaders(boolean enabled) {
        if (!isAvailable()) return;
        try {
            Object config = mGetConfig.invoke(api);
            mSetShadersEnabledAndApply.invoke(config, enabled);
        } catch (Throwable t) {
            // ignore — never crash the game over a shader toggle
        }
    }
}
