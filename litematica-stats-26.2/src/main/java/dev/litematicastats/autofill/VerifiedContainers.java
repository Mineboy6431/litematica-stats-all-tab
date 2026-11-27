package dev.litematicastats.autofill;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Containers the player has opened, and whether their contents differed from the schematic. */
final class VerifiedContainers {
    private static final Map<BlockPos, Boolean> STATE = new HashMap<>();
    private static Object level;

    private VerifiedContainers() {
    }

    static void syncLevel(Object current) {
        if (current != level) {
            STATE.clear();
            level = current;
        }
    }

    static void record(BlockPos pos, BlockPos partner, boolean wrong) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            syncLevel(mc.level);
        }
        STATE.put(pos.immutable(), wrong);
        if (partner != null) {
            STATE.put(partner.immutable(), wrong);
        }
    }

    static List<BlockPos> wrongPositions() {
        List<BlockPos> result = new ArrayList<>();
        for (Map.Entry<BlockPos, Boolean> entry : STATE.entrySet()) {
            if (entry.getValue()) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
}
