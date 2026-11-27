package dev.litematicastats.data;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.ListData;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Map;

/**
 * Replaces one item type by another everywhere in the containers of a schematic: inventories of
 * block entities and container entities, single item holders (jukeboxes, lecterns, item frames),
 * and items stored inside other items (shulker boxes, bundles). Slots and counts are kept; the
 * count is reduced when the new item has a smaller maximum stack size.
 */
public final class ItemReplacer {
    private static final String[] BLOCK_ENTITY_SINGLE_ITEM_KEYS = {"RecordItem", "item", "Book"};
    private static final String[] ENTITY_SINGLE_ITEM_KEYS = {"Item", "item"};

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
                    int changed = replaceInHolder(tag, BLOCK_ENTITY_SINGLE_ITEM_KEYS, fromId, toId, toMaxStack);
                    if (changed > 0) {
                        containers++;
                        stacks += changed;
                    }
                }
            }
            List<LitematicaSchematic.EntityInfo> entities = schematic.getEntityListForRegion(region);
            if (entities != null) {
                for (LitematicaSchematic.EntityInfo info : entities) {
                    int changed = replaceInHolder(info.nbt(), ENTITY_SINGLE_ITEM_KEYS, fromId, toId, toMaxStack);
                    if (changed > 0) {
                        containers++;
                        stacks += changed;
                    }
                }
            }
        }
        return new Result(containers, stacks);
    }

    private static int replaceInHolder(CompoundData tag, String[] singleItemKeys, String fromId, String toId, int toMaxStack) {
        int changed = 0;
        if (tag.contains("Items", 9)) {
            ListData items = tag.getList("Items");
            for (int i = 0; i < items.size(); i++) {
                changed += replaceInItem(items.getCompoundAt(i), fromId, toId, toMaxStack);
            }
        }
        for (String key : singleItemKeys) {
            if (tag.contains(key, 10)) {
                changed += replaceInItem(tag.getCompound(key), fromId, toId, toMaxStack);
            }
        }
        return changed;
    }

    private static int replaceInItem(CompoundData item, String fromId, String toId, int toMaxStack) {
        int changed = 0;
        if (fromId.equals(item.getString("id"))) {
            item.putString("id", toId);
            if (item.contains("count", 3) && item.getInt("count") > toMaxStack) {
                item.putInt("count", toMaxStack);
            }
            changed++;
        }
        if (item.contains("components", 10)) {
            CompoundData components = item.getCompound("components");
            if (components.contains("minecraft:container", 9)) {
                ListData slots = components.getList("minecraft:container");
                for (int i = 0; i < slots.size(); i++) {
                    CompoundData slot = slots.getCompoundAt(i);
                    if (slot.contains("item", 10)) {
                        changed += replaceInItem(slot.getCompound("item"), fromId, toId, toMaxStack);
                    }
                }
            }
            if (components.contains("minecraft:bundle_contents", 9)) {
                ListData contents = components.getList("minecraft:bundle_contents");
                for (int i = 0; i < contents.size(); i++) {
                    changed += replaceInItem(contents.getCompoundAt(i), fromId, toId, toMaxStack);
                }
            }
        }
        return changed;
    }
}
