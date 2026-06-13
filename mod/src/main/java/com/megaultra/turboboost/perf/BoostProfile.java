/*
 * MEGA Ultra Booster
 * Copyright (c) 2026 IAmCreatingSomeCheats. All Rights Reserved.
 * Proprietary software — unauthorized use, copying, modification, or
 * distribution of this source code is prohibited. See LICENSE for terms.
 */

package com.megaultra.turboboost.perf;

import com.google.gson.JsonObject;

/**
 * A bundle of vanilla video-option targets that the booster applies in one shot.
 * Defaults are the "aggressive" boost; the app can override any field over the link.
 */
public class BoostProfile {
    public int renderDistance = 8;        // <= 0 means "leave unchanged"
    public int maxFps = 0;                // 0 means unlimited (mapped to 260)
    public boolean graphicsFast = true;
    public boolean cloudsOff = true;
    public String particles = "minimal";  // all | decreased | minimal
    public boolean entityShadows = false; // desired state (false = shadows off)
    public boolean vsync = false;         // desired state (false = vsync off)
    public double entityDistance = 0.5;   // vanilla "Entity Distance" 0.5 - 1.0
    public boolean dynamicFps = true;

    /** The defaults already represent the aggressive one-key boost. */
    public static BoostProfile aggressive() {
        return new BoostProfile();
    }

    public static BoostProfile fromJson(JsonObject o) {
        BoostProfile p = new BoostProfile();
        if (o == null) return p;
        if (o.has("renderDistance")) p.renderDistance = o.get("renderDistance").getAsInt();
        if (o.has("maxFps")) p.maxFps = o.get("maxFps").getAsInt();
        if (o.has("graphicsFast")) p.graphicsFast = o.get("graphicsFast").getAsBoolean();
        if (o.has("cloudsOff")) p.cloudsOff = o.get("cloudsOff").getAsBoolean();
        if (o.has("particles")) p.particles = o.get("particles").getAsString();
        if (o.has("entityShadows")) p.entityShadows = o.get("entityShadows").getAsBoolean();
        if (o.has("vsync")) p.vsync = o.get("vsync").getAsBoolean();
        if (o.has("entityDistance")) p.entityDistance = o.get("entityDistance").getAsDouble();
        if (o.has("dynamicFps")) p.dynamicFps = o.get("dynamicFps").getAsBoolean();
        return p;
    }
}
