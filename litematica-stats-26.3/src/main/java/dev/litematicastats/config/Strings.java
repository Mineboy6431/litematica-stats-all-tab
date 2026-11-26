package dev.litematicastats.config;

import fi.dy.masa.malilib.util.StringUtils;

public final class Strings {
    private Strings() {
    }

    public static String translate(String key, Object... args) {
        return StringUtils.translate(key, args);
    }
}
