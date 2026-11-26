package dev.litematicastats.autofill;

import dev.litematicastats.config.StatsConfig;
import dev.litematicastats.util.Messages;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fills containers that are placed according to the loaded schematic.
 * Creative: the placed item carries the stored items. Survival: the container is opened and
 * the items are moved from the player's inventory into the slots given by the schematic.
 */
public final class AutoFiller {
    private static final int OPEN_DELAY_TICKS = 4;
    private static final int PENDING_TIMEOUT_TICKS = 40;
    private static final int JOB_TIMEOUT_TICKS = 100;
    private static final int SYNC_DELAY_TICKS = 3;

    private static BlockPos pendingPos;
    private static Block pendingBlock;
    private static int pendingAge;

    private static Job job;

    private static ItemStack restoreStack;
    private static InteractionHand restoreHand;
    private static int restoreSlot;
    private static int restoreAge;

    private AutoFiller() {
    }

    private static final class Job {
        final BlockPos pos;
        int age;
        int openTicks;
        List<ItemStack> wanted;
        int currentTarget = -1;
        int carriedFrom = -1;
        final Set<Integer> failed = new HashSet<>();

        Job(BlockPos pos) {
            this.pos = pos;
        }
    }

    public static void init() {
        UseBlockCallback.EVENT.register(AutoFiller::onUseBlock);
        ClientTickEvents.END_CLIENT_TICK.register(AutoFiller::onTick);
    }

    private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() || job != null) {
            return InteractionResult.PASS;
        }
        StatsConfig config = StatsConfig.get();
        if (!config.autoFillCreative() && !config.autoFillSurvival()) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof BlockItem blockItem)) {
            return InteractionResult.PASS;
        }
        BlockPlaceContext context = new BlockPlaceContext(player, hand, held, hit);
        if (!context.canPlace()) {
            return InteractionResult.PASS;
        }
        BlockPos pos = context.getClickedPos();
        List<ItemStack> contents = SchematicContainers.contentsAt(pos, blockItem.getBlock());
        if (contents == null) {
            return InteractionResult.PASS;
        }
        if (player.isCreative()) {
            if (config.autoFillCreative()) {
                injectCreative(player, hand, held, contents);
            }
        } else if (config.autoFillSurvival()) {
            pendingPos = pos.immutable();
            pendingBlock = blockItem.getBlock();
            pendingAge = 0;
        }
        return InteractionResult.PASS;
    }

    // ---- creative: the item that is placed already contains the stored items

    private static void injectCreative(Player player, InteractionHand hand, ItemStack held, List<ItemStack> contents) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) {
            return;
        }
        ItemStack injected = held.copy();
        injected.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        int slot = hand == InteractionHand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45;
        restoreStack = held.copy();
        restoreHand = hand;
        restoreSlot = slot;
        restoreAge = 0;
        player.setItemInHand(hand, injected);
        mc.gameMode.handleCreativeModeItemAdd(injected, slot);
    }

    private static void tickRestore(Minecraft mc, LocalPlayer player) {
        if (restoreStack == null) {
            return;
        }
        if (++restoreAge < 2) {
            return;
        }
        if (player.isCreative() && mc.gameMode != null) {
            player.setItemInHand(restoreHand, restoreStack);
            mc.gameMode.handleCreativeModeItemAdd(restoreStack, restoreSlot);
        }
        restoreStack = null;
    }

    // ---- survival: open the placed container and move the items in

    private static void onTick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.gameMode == null) {
            pendingPos = null;
            job = null;
            restoreStack = null;
            return;
        }
        tickRestore(mc, player);

        if (job == null && pendingPos != null) {
            pendingAge++;
            if (pendingAge > PENDING_TIMEOUT_TICKS) {
                pendingPos = null;
            } else if (pendingAge >= OPEN_DELAY_TICKS
                    && mc.level.getBlockState(pendingPos).getBlock() == pendingBlock
                    && mc.screen == null
                    && !player.isShiftKeyDown()
                    && player.distanceToSqr(Vec3.atCenterOf(pendingPos)) < 20.0) {
                job = new Job(pendingPos);
                pendingPos = null;
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(job.pos), Direction.UP, job.pos, false);
                mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
            }
        }
        if (job != null) {
            runJob(mc, player);
        }
    }

    private static boolean isSupported(AbstractContainerMenu menu) {
        return menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu
                || menu instanceof HopperMenu || menu instanceof DispenserMenu;
    }

    private static void runJob(Minecraft mc, LocalPlayer player) {
        Job current = job;
        if (++current.age > JOB_TIMEOUT_TICKS) {
            abort(player);
            return;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (!(mc.screen instanceof AbstractContainerScreen<?>) || menu == player.inventoryMenu) {
            if (current.age > 30) {
                job = null;
            }
            return;
        }
        if (!isSupported(menu)) {
            abort(player);
            return;
        }
        if (++current.openTicks < SYNC_DELAY_TICKS) {
            return;
        }
        int size = menu.slots.size() - 36;
        if (current.wanted == null) {
            current.wanted = SchematicContainers.contentsForMenu(current.pos, size);
            if (current.wanted == null) {
                abort(player);
                return;
            }
        }
        for (int n = 0; n < StatsConfig.get().clicksPerTick(); n++) {
            ItemStack carried = menu.getCarried();
            if (!carried.isEmpty()) {
                if (current.currentTarget < 0) {
                    abort(player);
                    return;
                }
                ItemStack want = current.wanted.get(current.currentTarget);
                ItemStack present = menu.getSlot(current.currentTarget).getItem();
                int need = want.getCount() - present.getCount();
                boolean sameAsWanted = ItemStack.isSameItemSameComponents(carried, want);
                boolean slotCompatible = present.isEmpty() || ItemStack.isSameItemSameComponents(present, want);
                if (need <= 0 || !sameAsWanted || !slotCompatible) {
                    click(mc, player, menu, current.carriedFrom, 0);
                    current.currentTarget = -1;
                } else if (carried.getCount() <= need) {
                    click(mc, player, menu, current.currentTarget, 0);
                } else {
                    click(mc, player, menu, current.currentTarget, 1);
                }
            } else {
                int target = nextTarget(menu, current, size);
                if (target < 0) {
                    finish(player);
                    return;
                }
                ItemStack want = current.wanted.get(target);
                int source = findSource(menu, size, want);
                if (source < 0) {
                    current.failed.add(target);
                    continue;
                }
                current.currentTarget = target;
                current.carriedFrom = source;
                click(mc, player, menu, source, 0);
            }
        }
    }

    private static int nextTarget(AbstractContainerMenu menu, Job current, int size) {
        for (int t = 0; t < size && t < current.wanted.size(); t++) {
            ItemStack want = current.wanted.get(t);
            if (want.isEmpty() || current.failed.contains(t)) {
                continue;
            }
            ItemStack present = menu.getSlot(t).getItem();
            if (present.isEmpty()
                    || (ItemStack.isSameItemSameComponents(present, want) && present.getCount() < want.getCount())) {
                return t;
            }
        }
        return -1;
    }

    private static int findSource(AbstractContainerMenu menu, int size, ItemStack want) {
        for (int i = size; i < menu.slots.size(); i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, want)) {
                return i;
            }
        }
        return -1;
    }

    private static void click(Minecraft mc, LocalPlayer player, AbstractContainerMenu menu, int slot, int button) {
        mc.gameMode.handleInventoryMouseClick(menu.containerId, slot, button, ClickType.PICKUP, player);
    }

    private static void finish(LocalPlayer player) {
        Job current = job;
        job = null;
        player.closeContainer();
        if (current != null && !current.failed.isEmpty()) {
            Messages.send(player, Component.literal(
                    dev.litematicastats.config.Strings.translate("litematica-stats.message.autofill.missing", current.failed.size())));
        }
    }

    private static void abort(LocalPlayer player) {
        job = null;
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
    }
}
