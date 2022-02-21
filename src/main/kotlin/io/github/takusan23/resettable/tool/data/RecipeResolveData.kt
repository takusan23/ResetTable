package io.github.takusan23.resettable.tool.data

import io.github.takusan23.resettable.tool.ResetTableTool
import net.minecraft.item.ItemStack

/**
 * [ResetTableTool.findCraftingMaterial]の返り値
 *
 * [recipePatternFormattedList]はこんなかんじの配列になってる
 *
 * [
 *   x,x,x,
 *   x, ,x,
 *   x,x,x
 * ]
 *
 *
 * @param recipePatternFormattedList レシピの配列
 * @param resolveSlotItemStack 戻したけどアイテム数が余った場合は入る
 * */
data class RecipeResolveData(
    val recipePatternFormattedList: List<ItemStack>,
    val resolveSlotItemStack: ItemStack,
)