package dev.litematicastats.crafting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Crafting steps and raw materials needed to get a set of items. */
public final class CraftingPlan {
    public record Step(String item, int needed, int crafts, int perCraft, int produced) {
    }

    public record Raw(String item, int needed) {
    }

    public final List<Step> steps = new ArrayList<>();
    public final List<Raw> raws = new ArrayList<>();

    private CraftingPlan() {
    }

    public static CraftingPlan compute(Map<String, Integer> topLevel) {
        RecipeBook book = RecipeBook.get();
        CraftingPlan plan = new CraftingPlan();

        Map<String, RecipeBook.Recipe> chosen = new HashMap<>();
        Map<String, Integer> indegree = new HashMap<>();
        Set<String> seen = new LinkedHashSet<>();
        Deque<String> work = new ArrayDeque<>(topLevel.keySet());
        while (!work.isEmpty()) {
            String item = work.poll();
            if (!seen.add(item)) {
                continue;
            }
            RecipeBook.Recipe recipe = book.choose(item);
            chosen.put(item, recipe);
            if (recipe != null) {
                for (String ingredient : recipe.ingredients().keySet()) {
                    indegree.merge(ingredient, 1, Integer::sum);
                    work.add(ingredient);
                }
            }
        }

        Map<String, Integer> need = new HashMap<>(topLevel);
        Deque<String> ready = new ArrayDeque<>();
        for (String item : seen) {
            if (indegree.getOrDefault(item, 0) == 0) {
                ready.add(item);
            }
        }
        while (!ready.isEmpty()) {
            String item = ready.poll();
            int needed = need.getOrDefault(item, 0);
            RecipeBook.Recipe recipe = chosen.get(item);
            if (recipe == null) {
                if (needed > 0) {
                    plan.raws.add(new Raw(item, needed));
                }
                continue;
            }
            int crafts = needed > 0 ? (needed + recipe.resultCount() - 1) / recipe.resultCount() : 0;
            if (crafts > 0) {
                plan.steps.add(new Step(item, needed, crafts, recipe.resultCount(), crafts * recipe.resultCount()));
            }
            for (Map.Entry<String, Integer> ingredient : recipe.ingredients().entrySet()) {
                need.merge(ingredient.getKey(), crafts * ingredient.getValue(), Integer::sum);
                if (indegree.merge(ingredient.getKey(), -1, Integer::sum) == 0) {
                    ready.add(ingredient.getKey());
                }
            }
        }
        return plan;
    }
}
