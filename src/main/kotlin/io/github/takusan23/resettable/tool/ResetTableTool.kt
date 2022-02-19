package io.github.takusan23.resettable.tool

import io.github.takusan23.resettable.tool.data.RecipeResolveData
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.item.ItemStack
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.world.World

/** このMODの目的となる作ったアイテムを戻すための関数がある */
object ResetTableTool {

    /** 赤色カラーコード */
    private val COLOR_RED = 0xFF0000

    /** 青色カラーコード */
    private val COLOR_BLUE = 0x0000FF

    /** [verifyResultItemRecipe]のレスポンス */
    enum class VerifyResult {
        /** ItemStackが空 */
        ERROR_EMPTY_ITEM_STACK,

        /** レシピが存在しない */
        ERROR_NOT_FOUND_RECIPE,

        /** スタック数が足りない */
        ERROR_REQUIRE_STACK_COUNT,

        /** エンチャント済みの道具はおそらくもとに戻さないだろう */
        ERROR_ENCHANTED_ITEM,

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
            EnchantmentHelper.get(resultItemStack).isNotEmpty() -> VerifyResult.ERROR_ENCHANTED_ITEM
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

    /**
     * [ResetTableTool.VerifyResult]からユーザー向けの説明を生成する
     *
     * @param result 列挙型のエラー
     * @return 文字と色のPair。nullの場合はアイテムが入ってないときで特にユーザー向けの説明はいらないかな
     * */
    fun resolveUserDescription(result: VerifyResult): Pair<String, Int>? {
        return when (result) {
            VerifyResult.ERROR_EMPTY_ITEM_STACK -> null // これは還元スロットが空の場合なので何もしない
            VerifyResult.ERROR_NOT_FOUND_RECIPE -> "レシピが存在しないようです" to COLOR_RED
            VerifyResult.ERROR_REQUIRE_STACK_COUNT -> "アイテム数が足りません" to COLOR_RED
            VerifyResult.ERROR_ENCHANTED_ITEM -> "エンチャント済みアイテムは戻せません" to COLOR_RED
            VerifyResult.SUCCESS -> "もとに戻せます" to COLOR_BLUE
        }
    }

}