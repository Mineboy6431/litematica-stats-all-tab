package dev.litematicastats.autofill;

import dev.litematicastats.config.StatsConfig;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Draws a box around opened containers whose contents differ from the schematic. */
final class WorldMarkers {
    private static final Logger LOGGER = LoggerFactory.getLogger("litematica-stats-all");
    private static boolean broken;

    private WorldMarkers() {
    }

    static void init() {
        LevelRenderEvents.BEFORE_GIZMOS.register(WorldMarkers::render);
    }

    private static void render(LevelRenderContext context) {
        if (broken) {
            return;
        }
        StatsConfig config = StatsConfig.get();
        if (!config.worldMarkers()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        VerifiedContainers.syncLevel(mc.level);
        try {
            int base = config.mismatchColor();
            GizmoStyle style = GizmoStyle.strokeAndFill(base | 0xFF000000, 3.0F, 0x55000000 | (base & 0x00FFFFFF));
            for (BlockPos pos : VerifiedContainers.wrongPositions()) {
                if (mc.level.getBlockState(pos).isAir()) {
                    continue;
                }
                Gizmos.cuboid(new AABB(pos).inflate(0.01), style);
            }
        } catch (RuntimeException e) {
            broken = true;
            LOGGER.warn("World markers disabled: {}", e.toString());
        }
    }
}
