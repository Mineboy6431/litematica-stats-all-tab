package dev.litematicastats.autofill;

import dev.litematicastats.config.StatsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Tints the slots of an opened container whose items differ from what the loaded schematic
 * has stored in that container (missing, wrong or extra items, or a wrong count).
 */
final class VerifyOverlay {
    private VerifyOverlay() {
    }

    static void render(Minecraft mc, AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics) {
        StatsConfig config = StatsConfig.get();
        if (!config.verifyOverlay()) {
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == player.inventoryMenu || menu != screen.getMenu()) {
            return;
        }
        BlockPos pos = AutoFiller.lastUsedPos();
        if (pos == null) {
            return;
        }
        int size = menu.slots.size() - 36;
        int imageHeight = imageHeight(menu);
        if (imageHeight < 0) {
            return;
        }
        List<ItemStack> wanted = SchematicContainers.contentsForMenu(pos, size);
        if (wanted == null) {
            return;
        }
        int left = (screen.width - 176) / 2;
        int top = (screen.height - imageHeight) / 2;
        int color = config.mismatchColor();
        for (int i = 0; i < size && i < wanted.size(); i++) {
            Slot slot = menu.getSlot(i);
            ItemStack want = wanted.get(i);
            ItemStack have = slot.getItem();
            boolean same = want.isEmpty()
                    ? have.isEmpty()
                    : ItemStack.isSameItemSameComponents(want, have) && want.getCount() == have.getCount();
            if (!same) {
                graphics.fill(left + slot.x, top + slot.y, left + slot.x + 16, top + slot.y + 16, color);
            }
        }
    }

    private static int imageHeight(AbstractContainerMenu menu) {
        if (menu instanceof ChestMenu chest) {
            return 114 + chest.getRowCount() * 18;
        }
        if (menu instanceof ShulkerBoxMenu) {
            return 167;
        }
        if (menu instanceof HopperMenu) {
            return 133;
        }
        if (menu instanceof DispenserMenu) {
            return 166;
        }
        return -1;
    }
}
