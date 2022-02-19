package io.github.takusan23.resettable.tool

import io.github.takusan23.resettable.tool.data.RecipeResolveData
import net.minecraft.item.ItemStack
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.world.World

/** このMODの目的となる作ったアイテムを戻すための関数がある */
object ResetTableTool {

    /** [verifyResultItemRecipe]のレスポンス */
    enum class VerifyResult {
        /** ItemStackが空 */
        ERROR_EMPTY_ITEM_STACK,

        /** レシピが存在しない */
        ERROR_NOT_FOUND_RECIPE,

        /** スタック数が足りない */
        ERROR_REQUIRE_STACK_COUNT,

        /** 戻せる */
        SUCCESS,
    }

    /**
     * 引数のアイテムがちゃんと戻せるか確認する関数
     *
     * @param world レシピを取得するのに使う
     * @param resultItemStack 検証するアイテム
     * @return [VerifyResult]
     * */
    fun verifyResultItemRecipe(world: World, resultItemStack: ItemStack): VerifyResult {
        if (resultItemStack == ItemStack.EMPTY) return VerifyResult.ERROR_EMPTY_ITEM_STACK

        val recipeManager = world.recipeManager.values()
        val materials = recipeManager
            // 作業台だけ
            .mapNotNull { it as? CraftingRecipe }
            // アイテムを確認する
            .find { it.output.item == resultItemStack.item }
        return when {
            materials == null -> VerifyResult.ERROR_NOT_FOUND_RECIPE
            materials.output!!.count > resultItemStack.count -> VerifyResult.ERROR_REQUIRE_STACK_COUNT
            else -> VerifyResult.SUCCESS
        }
    }

    /**
     * 作成するのに必要な材料を返す
     *
     * @param world レシピを取得するのに使う
     * @param resetItemStack 戻したいアイテム
     * @return レシピがない場合はnull
     * */
    fun findCraftingMaterial(world: World, resetItemStack: ItemStack): RecipeResolveData? {
        if (verifyResultItemRecipe(world, resetItemStack) != VerifyResult.SUCCESS) return null

        val recipeManager = world.recipeManager.values()
        // クラフトレシピを完成品から探す
        val recipe = recipeManager
            // 作業台だけ
            .mapNotNull { it as? CraftingRecipe }
            // アイテムとスタック数を確認する
            .find { it.output.item == resetItemStack.item && it.output.count <= resetItemStack.count }
        val resetItemStackCount = resetItemStack.count
        val recipeCreateItemCount = recipe?.output?.count ?: 0
        // 0で割ることがあるらしい
        return if (recipe != null && resetItemStackCount >= 1 && recipeCreateItemCount >= 1) {
            // 割り算して何個戻せるか
            val craftCount = resetItemStackCount / recipeCreateItemCount
            // 戻したけど余ったぶん
            val notResolveCount = resetItemStackCount % recipeCreateItemCount
            // 返す
            val materialList = recipe.ingredients
                .map { it.matchingStacks.getOrNull(0)?.copy()?.apply { count = craftCount } ?: ItemStack.EMPTY }
            val notResolveItemStack = resetItemStack.copy().apply { count = notResolveCount }
            RecipeResolveData(materialList, notResolveItemStack)
        } else null
    }

}