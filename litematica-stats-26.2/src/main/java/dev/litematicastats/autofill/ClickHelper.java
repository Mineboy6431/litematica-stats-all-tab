package dev.litematicastats.autofill;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Sends an inventory click. The click enum and the game mode method were renamed between Minecraft
 * versions, so both are looked up by name when first needed (26.x runs with the unobfuscated names).
 */
final class ClickHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("litematica-stats-all");
    private static Method method;
    private static Object pickup;
    private static boolean broken;

    private ClickHelper() {
    }

    static boolean broken() {
        return broken;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void init(Class<?> gameModeClass) throws ReflectiveOperationException {
        Class<?> inputClass = null;
        for (String name : new String[]{"net.minecraft.world.inventory.ContainerInput", "net.minecraft.world.inventory.ClickType"}) {
            try {
                inputClass = Class.forName(name);
                break;
            } catch (ClassNotFoundException ignored) {
            }
        }
        if (inputClass == null) {
            throw new ClassNotFoundException("container input class");
        }
        pickup = Enum.valueOf((Class) inputClass, "PICKUP");
        for (Method candidate : gameModeClass.getMethods()) {
            String name = candidate.getName();
            if ((name.equals("handleContainerInput") || name.equals("handleInventoryMouseClick"))
                    && candidate.getParameterCount() == 5) {
                method = candidate;
                return;
            }
        }
        throw new NoSuchMethodException("inventory click method");
    }

    static void click(Minecraft mc, LocalPlayer player, AbstractContainerMenu menu, int slot, int button) {
        try {
            if (method == null) {
                init(mc.gameMode.getClass());
            }
            method.invoke(mc.gameMode, menu.containerId, slot, button, pickup, player);
        } catch (ReflectiveOperationException | RuntimeException e) {
            broken = true;
            LOGGER.warn("Auto-fill disabled: cannot send inventory clicks ({})", e.toString());
        }
    }
}
