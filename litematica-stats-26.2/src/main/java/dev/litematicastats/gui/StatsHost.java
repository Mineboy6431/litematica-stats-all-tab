package dev.litematicastats.gui;

import dev.litematicastats.data.ContainerInfo;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface StatsHost {
    StatsTab getStatsTab();

    void setStatsTab(StatsTab tab);

    List<StatsEntry> buildStatsRows();

    List<StatsEntry> buildItemRows(ContainerInfo container);

    List<ContainerInfo> getEntityContainers(String entityId);

    int getStatsSortColumn();

    boolean isStatsSortReverse();

    void setStatsSort(int column);

    boolean isCategoryCollapsed(String key);

    void toggleCategory(String key);

    boolean isIgnored(ContainerInfo container);

    void toggleIgnored(ContainerInfo container);

    boolean isCategoryIgnored(String typeKey);

    void toggleCategoryIgnored(String typeKey);

    void setContentsViewWidth(int width);

    void setContentsFilter(String filter);

    boolean shouldShowRegion();

    boolean isAggregateItems();

    void toggleAggregateItems();

    int getMaxEntityNameWidth();

    int getMaxEntityCountWidth();

    int getMaxAggregateItemNameWidth();

    int getMaxAllNameWidth();

    /** Replaces the given item type in all containers of the schematic by the item held in the main hand. */
    void replaceItemInSchematic(ItemStack from);
}