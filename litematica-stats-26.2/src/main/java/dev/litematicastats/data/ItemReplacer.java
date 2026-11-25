package dev.litematicastats.data;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.ListData;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Map;

/**
 * Replaces one item type by another in the inventories of all containers of a schematic
 * (block entities and entities such as chest minecarts). Counts and slots are kept;
 * the count is reduced when the new item has a smaller maximum stack size.
 */
public final class ItemReplacer {
    private ItemReplacer() {
    }

    public record Result(int containers, int stacks) {
    }

    public static Result replace(LitematicaSchematic schematic, String fromId, String toId, int toMaxStack) {
        int containers = 0;
        int stacks = 0;
        for (String region : schematic.getAreas().keySet()) {
            Map<BlockPos, CompoundData> blockEntities = schematic.getBlockEntityMapForRegion(region);
            if (blockEntities != null) {
                for (CompoundData tag : blockEntities.values()) {
                    int changed = replaceInTag(tag, fromId, toId, toMaxStack);
                    if (changed > 0) {
                        containers++;
                        stacks += changed;
                    }
                }
            }
            List<LitematicaSchematic.EntityInfo> entities = schematic.getEntityListForRegion(region);
            if (entities != null) {
                for (LitematicaSchematic.EntityInfo info : entities) {
                    int changed = replaceInTag(info.nbt(), fromId, toId, toMaxStack);
                    if (changed > 0) {
                        containers++;
                        stacks += changed;
                    }
                }
            }
        }
        return new Result(containers, stacks);
    }

    private static int replaceInTag(CompoundData tag, String fromId, String toId, int toMaxStack) {
        if (!tag.contains("Items", 9)) {
            return 0;
        }
        ListData items = tag.getList("Items");
        int changed = 0;
        for (int i = 0; i < items.size(); i++) {
            CompoundData element = items.getCompoundAt(i);
            if (fromId.equals(element.getString("id"))) {
                element.putString("id", toId);
                if (element.contains("count", 3) && element.getInt("count") > toMaxStack) {
                    element.putInt("count", toMaxStack);
                }
                changed++;
            }
        }
        return changed;
    }
}
