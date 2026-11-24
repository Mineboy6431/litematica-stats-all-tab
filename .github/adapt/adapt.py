#!/usr/bin/env python3
"""Adapts the sources of a base port to a nearby Minecraft version.

Run from inside the project folder:  python3 adapt.py <profile>
Every replacement must match, otherwise the script fails loudly, so a change in the
base port can never silently produce a wrong build.
"""
import pathlib
import sys

SRC = pathlib.Path("src/main/java/dev/litematicastats")


def sub(rel, old, new, count=None):
    path = SRC / rel
    text = path.read_text(encoding="utf-8")
    found = text.count(old)
    if found == 0 or (count is not None and found != count):
        sys.exit(f"adapt: {rel}: found {found}x, expected {count if count is not None else '1 or more'}: {old[:90]!r}")
    path.write_text(text.replace(old, new), encoding="utf-8")


def sub_everywhere(old, new):
    changed = 0
    for path in SRC.rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        if old in text:
            path.write_text(text.replace(old, new), encoding="utf-8")
            changed += 1
    if changed == 0:
        sys.exit(f"adapt: pattern not found in any file: {old[:90]!r}")


# 1.18 / 1.18.1: RegistryAccess.fromRegistryOfRegistries was added in 1.18.2
def registry_access_pre_1_18_2():
    sub("data/StatsCollector.java",
        "RegistryAccess.fromRegistryOfRegistries(Registry.REGISTRY)",
        "RegistryAccess.builtin()", 1)


# 1.19.3 and older: BlockPos.containing and the PoseStack item renderer calls are 1.19.4 API
def old_item_rendering_and_blockpos():
    sub("data/StatsCollector.java",
        "BlockPos.containing(info.posVec)",
        "new BlockPos(net.minecraft.util.Mth.floor(info.posVec.x), "
        "net.minecraft.util.Mth.floor(info.posVec.y), net.minecraft.util.Mth.floor(info.posVec.z))", 1)
    sub("gui/WidgetStatsEntry.java",
        "this.mc.getItemRenderer().renderGuiItem(ps, stack, x, y);",
        "this.mc.getItemRenderer().renderGuiItem(stack, x, y);", 1)
    sub("gui/WidgetStatsEntry.java",
        "this.mc.getItemRenderer().renderGuiItemDecorations(ps, this.textRenderer, stack, x, y);",
        "this.mc.getItemRenderer().renderGuiItemDecorations(this.textRenderer, stack, x, y);", 1)


# 1.19.2 and older: BuiltInRegistries (1.19.3) did not exist yet, the constants live in Registry
def registries_pre_1_19_3():
    sub_everywhere("import net.minecraft.core.registries.BuiltInRegistries;", "import net.minecraft.core.Registry;")
    sub_everywhere("BuiltInRegistries.", "Registry.")


# 1.20.5 / 1.20.6: the 1.21 MaLiLib helpers used by the 1.21.1 port are not there yet
def malilib_gui_1_20_5():
    sub("gui/WidgetStatsEntry.java", "GuiUtils.getCurrentScreenWidth()", "GuiUtils.getScaledWindowWidth()", 1)
    sub("gui/WidgetStatsEntry.java", "GuiUtils.getCurrentScreenHeight()", "GuiUtils.getScaledWindowHeight()", 1)
    sub("gui/WidgetStatsEntry.java",
        "InventoryOverlay.renderStackToolTipStyled(mouseX + 10, mouseY, slot.stack(), this.mc, ctx)",
        "ctx.renderTooltip(this.mc.font, slot.stack(), mouseX + 10, mouseY)")
    sub("gui/WidgetStatsEntry.java",
        "InventoryOverlay.renderStackToolTipStyled(mouseX + 10, mouseY, this.entry.stack, this.mc, ctx)",
        "ctx.renderTooltip(this.mc.font, this.entry.stack, mouseX + 10, mouseY)")


# 1.21.2 - 1.21.4: MaLiLib takes the GuiGraphics as last argument, Registry#get returns an Optional
def malilib_ctx_last_and_registry_values():
    sub("gui/WidgetListStats.java",
        "capScrollbarTotal(totalHeight, scrollbarHeight));",
        "capScrollbarTotal(totalHeight, scrollbarHeight), ctx);", 1)
    sub("gui/WidgetStatsEntry.java",
        "this.renderColumnHeader(mouseX, mouseY, Icons.ARROW_DOWN, Icons.ARROW_UP);",
        "this.renderColumnHeader(mouseX, mouseY, Icons.ARROW_DOWN, Icons.ARROW_UP, ctx);", 1)
    sub("data/StatsCollector.java", "BuiltInRegistries.ENTITY_TYPE.get(rl)", "BuiltInRegistries.ENTITY_TYPE.getValue(rl)", 1)
    sub("data/StatsCollector.java", "BuiltInRegistries.ITEM.get(rl)", "BuiltInRegistries.ITEM.getValue(rl)", 1)


PROFILES = {
    "mc-1.18":   [registry_access_pre_1_18_2],
    "mc-1.19":   [old_item_rendering_and_blockpos, registries_pre_1_19_3],
    "mc-1.19.3": [old_item_rendering_and_blockpos],
    "mc-1.20.5": [malilib_gui_1_20_5],
    "mc-1.21.2": [malilib_ctx_last_and_registry_values],
}

if __name__ == "__main__":
    if len(sys.argv) != 2 or sys.argv[1] not in PROFILES:
        sys.exit(f"usage: adapt.py <{'|'.join(PROFILES)}>")
    for step in PROFILES[sys.argv[1]]:
        step()
        print("applied:", step.__name__)
