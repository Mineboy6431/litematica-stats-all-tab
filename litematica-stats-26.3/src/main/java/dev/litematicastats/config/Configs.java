package dev.litematicastats.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.config.options.ConfigString;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** MaLiLib config of the mod, stored in config/litematica-stats-all.json and shown in the settings screen. */
public final class Configs implements IConfigHandler {
    public static final String MOD_ID = "litematica-stats-all";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final String FILE_NAME = "litematica-stats-all.json";
    private static final String CATEGORY = "Settings";

    public static final ConfigString REPLACE_HOTKEY = new ConfigString("replaceHotkey", "CTRL+R",
            "Replaces the hovered item in all containers of the schematic with the item in your main hand.\n"
                    + "Modifiers CTRL, SHIFT, ALT plus a key name, for example CTRL+R or SHIFT+F6.",
            "Replace hotkey");
    public static final ConfigOptionList AUTO_FILL = new ConfigOptionList("autoFill", AutoFillMode.BOTH,
            "Fills containers when you place them according to the schematic.\n"
                    + "Creative: the placed item carries the stored items. Survival: the items are moved from your inventory.",
            "Auto-fill containers");
    public static final ConfigInteger CLICKS_PER_TICK = new ConfigInteger("clicksPerTick", 2, 1, 10,
            "Inventory clicks sent per tick while auto-filling in survival. Lower values are safer on servers.",
            "Auto-fill clicks per tick");
    public static final ConfigBoolean VERIFY_OVERLAY = new ConfigBoolean("verifyOverlay", true,
            "Tints the slots of an opened container that differ from the schematic.",
            "Highlight wrong slots");
    public static final ConfigBoolean WORLD_MARKERS = new ConfigBoolean("worldMarkers", true,
            "Marks containers you have opened and that contain wrong items with a box in the world.",
            "Mark wrong containers in the world");
    public static final ConfigColor MISMATCH_COLOR = new ConfigColor("mismatchColor", "#80000000",
            "Color of the slot highlight and of the box around containers with wrong contents (#AARRGGBB).",
            "Mismatch color");

    public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
            REPLACE_HOTKEY, AUTO_FILL, CLICKS_PER_TICK, VERIFY_OVERLAY, WORLD_MARKERS, MISMATCH_COLOR);

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    @Override
    public void load() {
        Path file = path();
        if (!Files.isReadable(file)) {
            return;
        }
        try {
            JsonElement element = JsonParser.parseString(Files.readString(file));
            if (element.isJsonObject()) {
                ConfigUtils.readConfigBase(element.getAsJsonObject(), CATEGORY, OPTIONS);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read {}: {}", file, e.toString());
        }
    }

    @Override
    public void save() {
        JsonObject root = new JsonObject();
        ConfigUtils.writeConfigBase(root, CATEGORY, OPTIONS);
        try {
            Files.writeString(path(), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root) + "\n");
        } catch (IOException e) {
            LOGGER.warn("Could not write {}: {}", path(), e.toString());
        }
    }
}
