package dev.litematicastats.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum AutoFillMode implements IConfigOptionListEntry {
    BOTH("both"),
    CREATIVE("creative"),
    SURVIVAL("survival"),
    OFF("off");

    private final String key;

    AutoFillMode(String key) {
        this.key = key;
    }

    @Override
    public String getStringValue() {
        return this.key;
    }

    @Override
    public String getDisplayName() {
        return StringUtils.translate("litematica-stats.config.autofill." + this.key);
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward) {
        AutoFillMode[] values = values();
        int next = this.ordinal() + (forward ? 1 : -1);
        if (next >= values.length) {
            next = 0;
        } else if (next < 0) {
            next = values.length - 1;
        }
        return values[next];
    }

    @Override
    public IConfigOptionListEntry fromString(String value) {
        for (AutoFillMode mode : values()) {
            if (mode.key.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return BOTH;
    }
}
