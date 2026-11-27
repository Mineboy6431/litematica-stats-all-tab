package dev.litematicastats;

import dev.litematicastats.autofill.AutoFiller;
import dev.litematicastats.config.InitHandler;
import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;

public class LitematicaStatsAll implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
        AutoFiller.init();
    }
}
