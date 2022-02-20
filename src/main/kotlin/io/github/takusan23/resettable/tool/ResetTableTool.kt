package io.github.takusan23.resettable.tool

import io.github.takusan23.resettable.tool.data.RecipeResolveData
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.item.ItemStack
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.text.TranslatableText
import net.minecraft.world.World

/** このMODの目的となる作ったアイテムを戻すための関数がある */
object ResetTableTool {

    /** 赤色カラーコード */
    private val COLOR_RED = 0xFF0000

    /** 青色カラーコード */
    private val COLOR_BLUE = 0x0000FF

    /**
     * [verifyResultItemRecipe]のレスポンス
     *
     * @param localizeKey ja_jp.json 等のローカライズでのキー
     * @param textColor 文字の色
     * */
    enum class VerifyResult(val localizeKey: String, val textColor: Int) {
        /** ItemStackが空っぽ。これはユーザーにエラーとしては表示しなくていい（アイテム入ってないエラーなので） */
        ERROR_EMPTY_ITEM_STACK("gui.resettable.error_empty_item_stack", COLOR_BLUE),

        /** 耐久度が減っている */
        ERROR_ITEM_DAMAGED("gui.resettable.error_item_damaged", COLOR_RED),

        /** レシピが存在しない */
        ERROR_NOT_FOUND_RECIPE("gui.resettable.error_not_found_recipe", COLOR_RED),

        /** スタック数が足りない */
        ERROR_REQUIRE_STACK_COUNT("gui.resettable.error_require_stack_count", COLOR_RED),

        /** エンチャント済みの道具はおそらくもとに戻さないだろう */
        ERROR_ENCHANTED_ITEM("gui.resettable.error_enchanted_item", COLOR_RED),

        /** 戻せる */
        SUCCESS("gui.resettable.successful", COLOR_BLUE),
    }

    /**
     * 引数のアイテムがちゃんと戻せるか確認する関数
     *
     * @param world レシピを取得するのに使う
     * @param resultItemStack 検証するアイテム
     * @return [VerifyResult]
     * */
    fun verifyResultItemRecipe(world: World, resultItemStack: ItemStack): VerifyResult {
        val recipeManager = world.recipeManager.values()
        val materials = recipeManager
            // 作業台だけ
            .mapNotNull { it as? CraftingRecipe }
            // アイテムを確認する
            .find { it.output.item == resultItemStack.item }

        return when {
            resultItemStack == ItemStack.EMPTY -> VerifyResult.ERROR_EMPTY_ITEM_STACK
            resultItemStack.isDamaged -> VerifyResult.ERROR_ITEM_DAMAGED
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
        // 還元スロットが空っぽのときのエラーは出さない
        if (result == VerifyResult.ERROR_EMPTY_ITEM_STACK) {
            return null
        }
        return TranslatableText(result.localizeKey).string to result.textColor
    }

}