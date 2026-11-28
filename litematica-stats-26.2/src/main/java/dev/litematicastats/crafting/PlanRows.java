package dev.litematicastats.crafting;

import dev.litematicastats.gui.StatsEntry;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Rows of the Crafting and Raw materials tabs. */
public final class PlanRows {
    private static Map<String, Item> items;

    private PlanRows() {
    }

    public record Result(List<StatsEntry> rows, int nameWidth, int primaryTotal, int secondaryTotal) {
    }

    private static synchronized Item item(String id) {
        if (items == null) {
            items = new HashMap<>();
            for (Item item : BuiltInRegistries.ITEM) {
                items.put(BuiltInRegistries.ITEM.getKey(item).toString(), item);
            }
        }
        return items.get(id);
    }

    private static int width(ItemStack stack) {
        return StringUtils.getStringWidth(stack.getHoverName().getString()) + 24;
    }

    /** One row per craftable item: needed, number of crafts, produced. */
    public static Result crafting(Map<String, Integer> needs) {
        CraftingPlan plan = CraftingPlan.compute(needs);
        List<StatsEntry> rows = new ArrayList<>();
        int nameWidth = 0;
        int crafts = 0;
        for (CraftingPlan.Step step : plan.steps) {
            Item item = item(step.item());
            if (item == null) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            StatsEntry row = StatsEntry.item(stack, step.needed());
            row.missing = step.crafts();
            row.available = step.produced();
            row.isBlock = true;
            row.planRow = true;
            rows.add(row);
            nameWidth = Math.max(nameWidth, width(stack));
            crafts += step.crafts();
        }
        return new Result(rows, nameWidth, rows.size(), crafts);
    }

    /** One row per raw material: needed, missing, available in the inventory. */
    public static Result raw(Map<String, Integer> needs, Map<Item, Integer> inventory) {
        CraftingPlan plan = CraftingPlan.compute(needs);
        List<CraftingPlan.Raw> raws = new ArrayList<>(plan.raws);
        raws.sort(Comparator.comparingInt(CraftingPlan.Raw::needed).reversed().thenComparing(CraftingPlan.Raw::item));
        List<StatsEntry> rows = new ArrayList<>();
        int nameWidth = 0;
        int total = 0;
        for (CraftingPlan.Raw raw : raws) {
            Item item = item(raw.item());
            if (item == null) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            int available = inventory.getOrDefault(item, 0);
            StatsEntry row = StatsEntry.item(stack, raw.needed());
            row.available = available;
            row.missing = Math.max(0, raw.needed() - available);
            row.isBlock = true;
            rows.add(row);
            nameWidth = Math.max(nameWidth, width(stack));
            total += raw.needed();
        }
        return new Result(rows, nameWidth, rows.size(), total);
    }
}
