package dev.litematicastats.autofill;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.util.ArrayList;
import java.util.List;

/** Reads the stored items of a container from the placed schematic (the schematic world). */
final class SchematicContainers {
    private SchematicContainers() {
    }

    /** Items of the container at the position when the schematic has the given block there, else null. */
    static List<ItemStack> contentsAt(BlockPos pos, Block expectedBlock) {
        var world = SchematicWorldHandler.getSchematicWorld();
        if (world == null || world.getBlockState(pos).getBlock() != expectedBlock) {
            return null;
        }
        if (!(world.getBlockEntity(pos) instanceof Container container)) {
            return null;
        }
        List<ItemStack> items = copyOf(container);
        return hasItems(items) ? items : null;
    }

    /** Items for an opened menu with the given number of container slots (handles double chests). */
    static List<ItemStack> contentsForMenu(BlockPos pos, int menuSize) {
        var world = SchematicWorldHandler.getSchematicWorld();
        if (world == null || !(world.getBlockEntity(pos) instanceof Container container)) {
            return null;
        }
        if (container.getContainerSize() == menuSize) {
            return copyOf(container);
        }
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE)
                && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos otherPos = pos.relative(ChestBlock.getConnectedDirection(state));
            if (world.getBlockEntity(otherPos) instanceof Container other
                    && container.getContainerSize() + other.getContainerSize() == menuSize) {
                boolean thisIsLeft = state.getValue(ChestBlock.TYPE) == ChestType.LEFT;
                List<ItemStack> items = new ArrayList<>(copyOf(thisIsLeft ? container : other));
                items.addAll(copyOf(thisIsLeft ? other : container));
                return items;
            }
        }
        return null;
    }

    private static List<ItemStack> copyOf(Container container) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            items.add(container.getItem(i).copy());
        }
        return items;
    }

    private static boolean hasItems(List<ItemStack> items) {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
