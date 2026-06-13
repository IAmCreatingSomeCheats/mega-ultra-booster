/*
 * MEGA Ultra Booster
 * Copyright (c) 2026 IAmCreatingSomeCheats. All Rights Reserved.
 * Proprietary software — unauthorized use, copying, modification, or
 * distribution of this source code is prohibited. See LICENSE for terms.
 */

package com.megaultra.turboboost.compat;

/**
 * Accessor for the active {@link TbCompat}. {@code CompatImpl} is provided by
 * exactly one per-version source overlay (legacy or modern) at build time.
 */
public final class Compat {

    private static final TbCompat INSTANCE = new CompatImpl();

    private Compat() {}

    public static TbCompat get() {
        return INSTANCE;
    }
}
