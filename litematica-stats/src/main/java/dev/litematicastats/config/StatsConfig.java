package dev.litematicastats.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
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
    // GLFW values, kept here so the config does not depend on the LWJGL classes at compile time
    private static final int KEY_R = 82;
    private static final int MOD_SHIFT = 1;
    private static final int MOD_CONTROL = 2;
    private static final int MOD_ALT = 4;
    private static StatsConfig instance;

    private int replaceKey = KEY_R;
    private int replaceModifiers = MOD_CONTROL;
    private boolean autoFillCreative = true;
    private boolean autoFillSurvival = true;
    private int clicksPerTick = 2;
    private boolean verifyOverlay = true;
    private int mismatchColor = 0x80000000;

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
                if (json.has("autoFill")) {
                    config.applyAutoFill(json.get("autoFill").getAsString());
                }
                if (json.has("verifyOverlay")) {
                    config.verifyOverlay = json.get("verifyOverlay").getAsBoolean();
                }
                if (json.has("mismatchColor")) {
                    config.applyMismatchColor(json.get("mismatchColor").getAsString());
                }
                if (json.has("clicksPerTick")) {
                    config.clicksPerTick = Math.max(1, Math.min(10, json.get("clicksPerTick").getAsInt()));
                }
            } else {
                JsonObject json = new JsonObject();
                json.addProperty("replaceHotkey", DEFAULT_REPLACE_HOTKEY);
                json.addProperty("autoFill", "both");
                json.addProperty("clicksPerTick", 2);
                json.addProperty("verifyOverlay", true);
                json.addProperty("mismatchColor", "#000000");
                Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(json) + "\n");
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read {}: {}", path, e.toString());
        }
        return config;
    }

    /** "#RRGGBB" (drawn 50% opaque) or "#AARRGGBB". */
    private void applyMismatchColor(String text) {
        String hex = text.trim().replace("#", "");
        try {
            long value = Long.parseLong(hex, 16);
            if (hex.length() == 6) {
                this.mismatchColor = (int) (0x80000000L | value);
            } else if (hex.length() == 8) {
                this.mismatchColor = (int) value;
            } else {
                LOGGER.warn("mismatchColor '{}' must be #RRGGBB or #AARRGGBB", text);
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("mismatchColor '{}' is not a valid color", text);
        }
    }

    private void applyAutoFill(String mode) {
        switch (mode.trim().toLowerCase(Locale.ROOT)) {
            case "off" -> {
                this.autoFillCreative = false;
                this.autoFillSurvival = false;
            }
            case "creative" -> {
                this.autoFillCreative = true;
                this.autoFillSurvival = false;
            }
            case "survival" -> {
                this.autoFillCreative = false;
                this.autoFillSurvival = true;
            }
            case "both" -> {
                this.autoFillCreative = true;
                this.autoFillSurvival = true;
            }
            default -> LOGGER.warn("Unknown autoFill value '{}' (use off, creative, survival or both)", mode);
        }
    }

    private void applyReplaceHotkey(String text) {
        int modifiers = 0;
        int key = -1;
        for (String part : text.split("\\+")) {
            String name = part.trim().toUpperCase(Locale.ROOT);
            switch (name) {
                case "" -> {
                }
                case "CTRL", "CONTROL" -> modifiers |= MOD_CONTROL;
                case "SHIFT" -> modifiers |= MOD_SHIFT;
                case "ALT" -> modifiers |= MOD_ALT;
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
            return Class.forName("org.lwjgl.glfw.GLFW").getField("GLFW_KEY_" + name).getInt(null);
        } catch (ReflectiveOperationException e) {
            return -1;
        }
    }

    public boolean isReplaceHotkey(int key, int modifiers) {
        int mask = MOD_CONTROL | MOD_SHIFT | MOD_ALT;
        return key == this.replaceKey && (modifiers & mask) == this.replaceModifiers;
    }

    public boolean autoFillCreative() {
        return this.autoFillCreative;
    }

    public boolean autoFillSurvival() {
        return this.autoFillSurvival;
    }

    public boolean verifyOverlay() {
        return this.verifyOverlay;
    }

    public int mismatchColor() {
        return this.mismatchColor;
    }

    public int clicksPerTick() {
        return this.clicksPerTick;
    }
}
