package dev.litematicastats.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Small JSON config: .minecraft/config/litematica-stats-all.json
 * The file is created with defaults on first use. Changes are picked up after a restart.
 */
public final class StatsConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("litematica-stats-all");
    private static final String FILE_NAME = "litematica-stats-all.json";
    private static final String DEFAULT_REPLACE_HOTKEY = "CTRL+R";
    private static StatsConfig instance;

    private int replaceKey = GLFW.GLFW_KEY_R;
    private int replaceModifiers = GLFW.GLFW_MOD_CONTROL;

    private StatsConfig() {
    }

    public static StatsConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static StatsConfig load() {
        StatsConfig config = new StatsConfig();
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try {
            if (Files.exists(path)) {
                JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                if (json.has("replaceHotkey")) {
                    config.applyReplaceHotkey(json.get("replaceHotkey").getAsString());
                }
            } else {
                JsonObject json = new JsonObject();
                json.addProperty("replaceHotkey", DEFAULT_REPLACE_HOTKEY);
                Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(json) + "\n");
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read {}: {}", path, e.toString());
        }
        return config;
    }

    private void applyReplaceHotkey(String text) {
        int modifiers = 0;
        int key = -1;
        for (String part : text.split("\\+")) {
            String name = part.trim().toUpperCase(Locale.ROOT);
            switch (name) {
                case "" -> {
                }
                case "CTRL", "CONTROL" -> modifiers |= GLFW.GLFW_MOD_CONTROL;
                case "SHIFT" -> modifiers |= GLFW.GLFW_MOD_SHIFT;
                case "ALT" -> modifiers |= GLFW.GLFW_MOD_ALT;
                default -> key = keyFromName(name);
            }
        }
        if (key == -1) {
            LOGGER.warn("Unknown replaceHotkey '{}', using {}", text, DEFAULT_REPLACE_HOTKEY);
            return;
        }
        this.replaceKey = key;
        this.replaceModifiers = modifiers;
    }

    private static int keyFromName(String name) {
        try {
            return GLFW.class.getField("GLFW_KEY_" + name).getInt(null);
        } catch (ReflectiveOperationException e) {
            return -1;
        }
    }

    public boolean isReplaceHotkey(int key, int modifiers) {
        int mask = GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_ALT;
        return key == this.replaceKey && (modifiers & mask) == this.replaceModifiers;
    }
}
