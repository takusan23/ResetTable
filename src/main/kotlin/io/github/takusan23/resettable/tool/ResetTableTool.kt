package io.github.takusan23.resettable.tool

import io.github.takusan23.resettable.tool.data.RecipeResolveData
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.recipe.ShapedRecipe
import net.minecraft.text.Text
import net.minecraft.world.World

/** このMODの目的となる作ったアイテムを戻すための関数がある */
object ResetTableTool {

    /** 赤色カラーコード */
    private const val COLOR_RED = 0xFF0000

    /** 青色カラーコード */
    private const val COLOR_BLUE = 0x0000FF

    /**
     * [verifyResultItemRecipe]のレスポンス
     *
     * @param localizeKey ja_jp.json 等のローカライズでのキー
     * @param textColor 文字の色
     */
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

        /** シュルカーボックスなんかのアイテムは戻せないように */
        ERROR_HAS_METADATA("gui.resettable.error_has_metadata", COLOR_RED),

        /** 戻せる */
        SUCCESS("gui.resettable.successful", COLOR_BLUE),
    }

    /**
     * レシピを探して返す。アイテム数があってるかとかは見ていない
     *
     * @param world レシピもらうのに使う
     * @param resetItemStack 探すアイテム
     * @return レシピの配列
     */
    private fun findRecipe(world: World, resetItemStack: ItemStack): List<CraftingRecipe> {
        val recipeManager = world.recipeManager.values()
        return recipeManager
            // ID と Recipe の Map になってる、Recipe だけにする
            .map { it.value }
            // 作業台だけ
            .filterIsInstance<CraftingRecipe>()
            // クラフトレシピを完成品から探す
            .filter { it.getResult(null).item == resetItemStack.item }
    }

    /**
     * 引数のアイテムがちゃんと戻せるか確認する関数
     *
     * @param world レシピを取得するのに使う
     * @param resultItemStack 検証するアイテム
     * @return [VerifyResult]
     */
    fun verifyResultItemRecipe(world: World, resultItemStack: ItemStack): VerifyResult {
        val recipeList = findRecipe(world, resultItemStack)
        val availableRecipe = recipeList.firstOrNull { it.getResult(null).count <= resultItemStack.count }

        return when {
            resultItemStack == ItemStack.EMPTY -> VerifyResult.ERROR_EMPTY_ITEM_STACK
            recipeList.isEmpty() -> VerifyResult.ERROR_NOT_FOUND_RECIPE
            resultItemStack.isDamaged -> VerifyResult.ERROR_ITEM_DAMAGED
            EnchantmentHelper.get(resultItemStack).isNotEmpty() -> VerifyResult.ERROR_ENCHANTED_ITEM
            availableRecipe == null -> VerifyResult.ERROR_REQUIRE_STACK_COUNT
            BlockItem.getBlockEntityNbt(resultItemStack)?.isEmpty == false -> VerifyResult.ERROR_HAS_METADATA
            else -> VerifyResult.SUCCESS
        }
    }

    /**
     * 作成するのに必要な材料を返す
     *
     * @param world レシピを取得するのに使う
     * @param resetItemStack 戻したいアイテム
     * @return [verifyResultItemRecipe]で成功を返さなかった場合はnull
     */
    fun findCraftingMaterial(world: World, resetItemStack: ItemStack): List<RecipeResolveData>? {
        // 検証した結果もとに戻せない場合はnullを返す
        if (verifyResultItemRecipe(world, resetItemStack) != VerifyResult.SUCCESS) return null

        // クラフトレシピを完成品から探す
        val recipeList = findRecipe(world, resetItemStack)
            // スタック数を確認する
            // 同じ完成品のレシピで複数返す場合に備えて
            .filter { it.getResult(null).count <= resetItemStack.count }

        val recipeResolvedDataList = recipeList.map { recipe ->
            val resetItemStackCount = resetItemStack.count
            val recipeCreateItemCount = recipe.getResult(null)?.count ?: 0
            // 0で割ることがあるらしい
            if (resetItemStackCount >= 1 && recipeCreateItemCount >= 1) {
                // 割り算して何個戻せるか
                val craftCount = resetItemStackCount / recipeCreateItemCount
                // 戻したけど余ったぶん
                val notResolveCount = resetItemStackCount % recipeCreateItemCount
                // 返す
                val notResolveItemStack = resetItemStack.copy().apply { count = notResolveCount }
                val materialList = recipe.ingredients
                    .map { it.matchingStacks.getOrNull(0)?.copy()?.apply { count = craftCount } ?: ItemStack.EMPTY }

                // 定形レシピの場合は材料スロット(3x3)で正しいアイテムの配列に置き換える
                if (recipe is ShapedRecipe) {
                    // 作成で使う縦、横のスロット数
                    val patternWidth = recipe.width
                    val patternHeight = recipe.height
                    // レシピの形に整形した配列
                    val recipePatternList = mutableListOf<ItemStack>()

                    //  例えば剣のレシピがこうで
                    //
                    // X
                    // X
                    // Y
                    //
                    // 普通に材料を取得するとこうなる
                    // [X,X,Y]
                    //
                    // これだとレシピの形になっていないのでこんな感じの配列にする。(以下の例は改行してるけど)
                    // [
                    //  X,empty,empty,
                    //  X,empty,empty,
                    //  Y,empty,empty
                    // ]
                    var prevPos = 0
                    repeat(patternHeight) { height ->
                        // ここで各横スロットのアイテムを一斉に入れている
                        // prevPosには各横スロットの最後のIndexが入ってる
                        recipePatternList.addAll(materialList.subList(prevPos, prevPos + patternWidth))
                        // 幅が3未満の場合は残りを空のアイテムで埋める
                        repeat(3 - patternWidth) {
                            recipePatternList.add(ItemStack.EMPTY)
                        }
                        prevPos += patternWidth
                    }
                    RecipeResolveData(recipePatternList, notResolveItemStack)
                } else {
                    RecipeResolveData(materialList, notResolveItemStack)
                }
            } else null
        }
        return recipeResolvedDataList.filterNotNull()
    }

    /**
     * [ResetTableTool.VerifyResult]からユーザー向けの説明を生成する
     *
     * @param result 列挙型のエラー
     * @return 文字と色のPair。nullの場合はアイテムが入ってないときで特にユーザー向けの説明はいらないかな
     */
    fun resolveUserDescription(result: VerifyResult): Pair<String, Int>? {
        // 還元スロットが空っぽのときのエラーは出さない
        if (result == VerifyResult.ERROR_EMPTY_ITEM_STACK) {
            return null
        }
        return Text.translatable(result.localizeKey).string to result.textColor
    }

}