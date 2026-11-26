package dev.litematicastats;

import dev.litematicastats.autofill.AutoFiller;
import net.fabricmc.api.ClientModInitializer;

public class LitematicaStatsAll implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AutoFiller.init();
    }
}
