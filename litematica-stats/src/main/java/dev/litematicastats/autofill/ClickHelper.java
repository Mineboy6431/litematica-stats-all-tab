package dev.litematicastats.autofill;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;

final class ClickHelper {
    private ClickHelper() {
    }

    static boolean broken() {
        return false;
    }

    static void click(Minecraft mc, LocalPlayer player, AbstractContainerMenu menu, int slot, int button) {
        mc.gameMode.handleInventoryMouseClick(menu.containerId, slot, button, ClickType.PICKUP, player);
    }
}
