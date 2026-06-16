/*
 * MEGA Ultra Booster
 * Copyright (c) 2026 IAmCreatingSomeCheats. All Rights Reserved.
 * Proprietary software — unauthorized use, copying, modification, or
 * distribution of this source code is prohibited. See LICENSE for terms.
 */

package com.megaultra.turboboost.server;

import com.megaultra.turboboost.TurboBoostClient;
import com.megaultra.turboboost.compat.Compat;
import net.minecraft.client.MinecraftClient;

/** Disconnects from the current server (if any) and connects to a new one. */
public final class ServerSwitcher {

    private ServerSwitcher() {}

    public static void connect(String name, String address) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            TurboBoostClient.actionBar("§b⚡ Switching to §f" + name + " §7(" + address + ")");
            // The disconnect + ConnectScreen.connect signatures vary by MC version,
            // so the whole leave-and-connect lives in the per-version compat overlay.
            Compat.get().connectToServer(client, name, address);
        });
    }
}
