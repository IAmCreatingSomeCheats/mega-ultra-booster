/*
 * MEGA Ultra Booster
 * Copyright (c) 2026 IAmCreatingSomeCheats. All Rights Reserved.
 * Proprietary software — unauthorized use, copying, modification, or
 * distribution of this source code is prohibited. See LICENSE for terms.
 */

package com.megaultra.turboboost.server;

import com.megaultra.turboboost.TurboBoostClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

/** Disconnects from the current server (if any) and connects to a new one. */
public final class ServerSwitcher {

    private ServerSwitcher() {}

    public static void connect(String name, String address) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> doConnect(client, name, address));
    }

    private static void doConnect(MinecraftClient client, String name, String address) {
        TurboBoostClient.actionBar("§b⚡ Switching to §f" + name + " §7(" + address + ")");

        // Leave the current world/server first (no-op at the main menu).
        if (client.world != null) {
            client.disconnectWithProgressScreen();
        }

        ServerAddress addr = ServerAddress.parse(address);
        ServerInfo info = new ServerInfo(name, address, ServerInfo.ServerType.OTHER);

        // VERSION-SENSITIVE: ConnectScreen.connect's signature changes between MC
        // versions. For 1.21.11 it is:
        //   connect(Screen, MinecraftClient, ServerAddress, ServerInfo, boolean, CookieStorage)
        // If a build error points here, check the current signature on Linkie.
        ConnectScreen.connect(
                new MultiplayerScreen(new TitleScreen()),
                client, addr, info, false, null);
    }
}
