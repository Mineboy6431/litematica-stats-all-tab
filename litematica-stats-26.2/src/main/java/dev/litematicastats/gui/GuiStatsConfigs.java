package dev.litematicastats.gui;

import dev.litematicastats.config.Configs;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

/** Settings screen of the mod, built like Litematica's own configuration screens. */
public class GuiStatsConfigs extends GuiConfigsBase {
    public GuiStatsConfigs(Screen parent) {
        super(10, 50, Configs.MOD_ID, parent, "litematica-stats.gui.title.configs");
    }

    @Override
    public List<GuiConfigsBase.ConfigOptionWrapper> getConfigs() {
        return GuiConfigsBase.ConfigOptionWrapper.createFor(Configs.OPTIONS);
    }
}
