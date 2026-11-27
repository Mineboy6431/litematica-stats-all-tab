package dev.litematicastats.config;

import java.util.Locale;

/** Read access to the mod settings (see Configs); also parses the replace hotkey text. */
public final class StatsConfig {
    private static final StatsConfig INSTANCE = new StatsConfig();
    private static final int KEY_R = 82;
    private static final int MOD_SHIFT = 1;
    private static final int MOD_CONTROL = 2;
    private static final int MOD_ALT = 4;

    private String parsedText;
    private int replaceKey = KEY_R;
    private int replaceModifiers = MOD_CONTROL;

    private StatsConfig() {
    }

    public static StatsConfig get() {
        return INSTANCE;
    }

    private void parseHotkey() {
        String text = Configs.REPLACE_HOTKEY.getStringValue();
        if (text.equals(this.parsedText)) {
            return;
        }
        this.parsedText = text;
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
            this.replaceKey = KEY_R;
            this.replaceModifiers = MOD_CONTROL;
        } else {
            this.replaceKey = key;
            this.replaceModifiers = modifiers;
        }
    }

    private static int keyFromName(String name) {
        try {
            return Class.forName("org.lwjgl.glfw.GLFW").getField("GLFW_KEY_" + name).getInt(null);
        } catch (ReflectiveOperationException e) {
            return -1;
        }
    }

    public boolean isReplaceHotkey(int key, int modifiers) {
        this.parseHotkey();
        int mask = MOD_CONTROL | MOD_SHIFT | MOD_ALT;
        return key == this.replaceKey && (modifiers & mask) == this.replaceModifiers;
    }

    private static AutoFillMode mode() {
        return Configs.AUTO_FILL.getOptionListValue() instanceof AutoFillMode mode ? mode : AutoFillMode.BOTH;
    }

    public boolean autoFillCreative() {
        AutoFillMode mode = mode();
        return mode == AutoFillMode.BOTH || mode == AutoFillMode.CREATIVE;
    }

    public boolean autoFillSurvival() {
        AutoFillMode mode = mode();
        return mode == AutoFillMode.BOTH || mode == AutoFillMode.SURVIVAL;
    }

    public int clicksPerTick() {
        return Configs.CLICKS_PER_TICK.getIntegerValue();
    }

    public boolean verifyOverlay() {
        return Configs.VERIFY_OVERLAY.getBooleanValue();
    }

    public boolean worldMarkers() {
        return Configs.WORLD_MARKERS.getBooleanValue();
    }

    public int mismatchColor() {
        return Configs.MISMATCH_COLOR.getIntegerValue();
    }
}
