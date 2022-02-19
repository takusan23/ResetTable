package io.github.takusan23.resettable.tool.data

import io.github.takusan23.resettable.tool.ResetTableTool
import net.minecraft.item.ItemStack

/**
 * [ResetTableTool.findCraftingMaterial]の返り値
 *
 * @param resultItemStack 材料
 * @param resolveSlotItemStack 戻したけどアイテム数が余った場合は入る
 * */
data class RecipeResolveData(
    val resultItemStack: List<ItemStack>,
    val resolveSlotItemStack: ItemStack,
)