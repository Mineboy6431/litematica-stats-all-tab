package dev.litematicastats.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Crafting recipes of the game, read from the data files of the Minecraft jar (shaped and shapeless
 * crafting recipes). Where an ingredient is a tag, its first item is used. Recipes that only convert
 * between compressed and uncompressed forms (ingot, block, nugget) are left out, so that for example
 * iron ingots count as raw material and iron blocks are crafted from nine of them.
 */
public final class RecipeBook {
    private static final Logger LOGGER = LoggerFactory.getLogger("litematica-stats-all");
    private static RecipeBook instance;

    public record Recipe(String id, String result, int resultCount, Map<String, Integer> ingredients) {
        int ingredientTotal() {
            int total = 0;
            for (int count : this.ingredients.values()) {
                total += count;
            }
            return total;
        }
    }

    private final Map<String, List<String>> tags = new HashMap<>();
    private final Map<String, List<Recipe>> byResult = new HashMap<>();
    private final Map<String, Recipe> chosen = new HashMap<>();

    public static synchronized RecipeBook get() {
        if (instance == null) {
            instance = new RecipeBook();
        }
        return instance;
    }

    private RecipeBook() {
        try {
            this.loadTags();
            List<Recipe> usable = this.filter(this.loadRecipes());
            for (Recipe recipe : usable) {
                this.byResult.computeIfAbsent(recipe.result(), k -> new ArrayList<>()).add(recipe);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read the crafting recipes: {}", e.toString());
        }
        LOGGER.info("Crafting plan: {} items with a recipe", this.byResult.size());
    }

    /** The recipe used to craft the item, or null when the item is a raw material. */
    public Recipe choose(String item) {
        if (this.chosen.containsKey(item)) {
            return this.chosen.get(item);
        }
        Recipe best = null;
        double bestRatio = -1.0;
        for (Recipe recipe : this.byResult.getOrDefault(item, List.of())) {
            double ratio = (double) recipe.resultCount() / Math.max(1, recipe.ingredientTotal());
            if (best == null || ratio > bestRatio || (ratio == bestRatio && recipe.id().compareTo(best.id()) < 0)) {
                best = recipe;
                bestRatio = ratio;
            }
        }
        this.chosen.put(item, best);
        return best;
    }

    // ---- reading the data files

    private static Map<String, Path> jsonFiles(String... directories) throws IOException {
        Map<String, Path> files = new TreeMap<>();
        ModContainer minecraft = FabricLoader.getInstance().getModContainer("minecraft").orElse(null);
        if (minecraft == null) {
            return files;
        }
        for (String directory : directories) {
            Path root = minecraft.findPath(directory).orElse(null);
            if (root == null) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(root)) {
                for (Path file : (Iterable<Path>) stream::iterator) {
                    String name = root.relativize(file).toString().replace('\\', '/');
                    if (name.endsWith(".json")) {
                        files.put("minecraft:" + name.substring(0, name.length() - 5), file);
                    }
                }
            }
            if (!files.isEmpty()) {
                break;
            }
        }
        return files;
    }

    private void loadTags() throws IOException {
        for (Map.Entry<String, Path> file : jsonFiles("data/minecraft/tags/item", "data/minecraft/tags/items").entrySet()) {
            JsonObject json = JsonParser.parseString(Files.readString(file.getValue())).getAsJsonObject();
            List<String> values = new ArrayList<>();
            if (json.has("values")) {
                for (JsonElement value : json.getAsJsonArray("values")) {
                    if (value.isJsonPrimitive()) {
                        values.add(value.getAsString());
                    } else if (value.isJsonObject() && value.getAsJsonObject().has("id")) {
                        values.add(value.getAsJsonObject().get("id").getAsString());
                    }
                }
            }
            this.tags.put(file.getKey(), values);
        }
    }

    private List<String> resolveTag(String tag, Set<String> visiting) {
        List<String> result = new ArrayList<>();
        if (!visiting.add(tag)) {
            return result;
        }
        for (String value : this.tags.getOrDefault(tag, List.of())) {
            if (value.startsWith("#")) {
                result.addAll(this.resolveTag(value.substring(1), visiting));
            } else {
                result.add(value);
            }
        }
        visiting.remove(tag);
        return result;
    }

    private List<String> options(JsonElement element) {
        List<String> result = new ArrayList<>();
        if (element == null) {
            return result;
        }
        if (element.isJsonPrimitive()) {
            String value = element.getAsString();
            if (value.startsWith("#")) {
                result.addAll(this.resolveTag(value.substring(1), new HashSet<>()));
            } else {
                result.add(value);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                result.addAll(this.options(child));
            }
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("item")) {
                result.add(object.get("item").getAsString());
            } else if (object.has("tag")) {
                result.addAll(this.resolveTag(object.get("tag").getAsString(), new HashSet<>()));
            } else if (object.has("id")) {
                result.add(object.get("id").getAsString());
            }
        }
        return result;
    }

    private String representative(JsonElement ingredient) {
        List<String> options = this.options(ingredient);
        return options.isEmpty() ? null : options.get(0);
    }

    private List<Recipe> loadRecipes() throws IOException {
        List<Recipe> recipes = new ArrayList<>();
        for (Map.Entry<String, Path> file : jsonFiles("data/minecraft/recipe", "data/minecraft/recipes").entrySet()) {
            JsonObject json = JsonParser.parseString(Files.readString(file.getValue())).getAsJsonObject();
            if (!json.has("type") || !json.has("result")) {
                continue;
            }
            String type = json.get("type").getAsString().replace("minecraft:", "");
            if (!type.equals("crafting_shaped") && !type.equals("crafting_shapeless")) {
                continue;
            }
            JsonElement resultElement = json.get("result");
            String result;
            int count = 1;
            if (resultElement.isJsonPrimitive()) {
                result = resultElement.getAsString();
            } else {
                JsonObject object = resultElement.getAsJsonObject();
                JsonElement id = object.has("id") ? object.get("id") : object.get("item");
                if (id == null) {
                    continue;
                }
                result = id.getAsString();
                if (object.has("count")) {
                    count = object.get("count").getAsInt();
                }
            }
            Map<String, Integer> ingredients = new HashMap<>();
            boolean valid = true;
            if (type.equals("crafting_shaped")) {
                JsonObject key = json.getAsJsonObject("key");
                JsonArray pattern = json.getAsJsonArray("pattern");
                Map<Character, Integer> counts = new HashMap<>();
                for (JsonElement row : pattern) {
                    for (char c : row.getAsString().toCharArray()) {
                        if (c != ' ') {
                            counts.merge(c, 1, Integer::sum);
                        }
                    }
                }
                for (Map.Entry<Character, Integer> entry : counts.entrySet()) {
                    String item = this.representative(key.get(String.valueOf(entry.getKey())));
                    if (item == null) {
                        valid = false;
                        break;
                    }
                    ingredients.merge(item, entry.getValue(), Integer::sum);
                }
            } else {
                for (JsonElement ingredient : json.getAsJsonArray("ingredients")) {
                    String item = this.representative(ingredient);
                    if (item == null) {
                        valid = false;
                        break;
                    }
                    ingredients.merge(item, 1, Integer::sum);
                }
            }
            if (valid && !ingredients.isEmpty()) {
                recipes.add(new Recipe(file.getKey(), result, Math.max(1, count), ingredients));
            }
        }
        return recipes;
    }

    // ---- removing recipes that only convert between compressed and uncompressed forms

    private static Map<String, Set<String>> graph(List<Recipe> recipes) {
        Map<String, Set<String>> graph = new HashMap<>();
        for (Recipe recipe : recipes) {
            graph.computeIfAbsent(recipe.result(), k -> new HashSet<>()).addAll(recipe.ingredients().keySet());
        }
        return graph;
    }

    private static boolean reaches(Map<String, Set<String>> graph, String from, String target) {
        if (from.equals(target)) {
            return true;
        }
        Set<String> seen = new HashSet<>();
        Deque<String> work = new ArrayDeque<>();
        work.add(from);
        while (!work.isEmpty()) {
            String item = work.poll();
            if (!seen.add(item)) {
                continue;
            }
            for (String next : graph.getOrDefault(item, Set.of())) {
                if (next.equals(target)) {
                    return true;
                }
                work.add(next);
            }
        }
        return false;
    }

    private static boolean isCyclic(Map<String, Set<String>> graph, Recipe recipe) {
        for (String ingredient : recipe.ingredients().keySet()) {
            if (reaches(graph, ingredient, recipe.result())) {
                return true;
            }
        }
        return false;
    }

    private List<Recipe> filter(List<Recipe> all) {
        List<Recipe> usable = new ArrayList<>();
        for (Recipe recipe : all) {
            boolean usesNugget = false;
            for (String ingredient : recipe.ingredients().keySet()) {
                if (ingredient.endsWith("_nugget")) {
                    usesNugget = true;
                    break;
                }
            }
            if (!usesNugget) {
                usable.add(recipe);
            }
        }
        // 1. decompression recipes (one block gives several items) that lead back to their own result
        boolean changed = true;
        for (int pass = 0; changed && pass < 10; pass++) {
            changed = false;
            Map<String, Set<String>> graph = graph(usable);
            List<Recipe> remove = new ArrayList<>();
            for (Recipe recipe : usable) {
                boolean decompress = recipe.ingredients().size() == 1 && recipe.ingredientTotal() == 1 && recipe.resultCount() > 1;
                if (decompress && isCyclic(graph, recipe)) {
                    remove.add(recipe);
                }
            }
            if (!remove.isEmpty()) {
                usable.removeAll(remove);
                changed = true;
            }
        }
        // 2. anything that is still part of a cycle
        changed = true;
        for (int pass = 0; changed && pass < 10; pass++) {
            changed = false;
            Map<String, Set<String>> graph = graph(usable);
            List<Recipe> remove = new ArrayList<>();
            for (Recipe recipe : usable) {
                if (isCyclic(graph, recipe)) {
                    remove.add(recipe);
                }
            }
            if (!remove.isEmpty()) {
                usable.removeAll(remove);
                changed = true;
            }
        }
        return usable;
    }
}
