package dev.litematicastats.util;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public final class Messages {
    private Messages() {
    }

    public static void send(LocalPlayer player, Component text) {
        player.displayClientMessage(text, true);
    }
}
