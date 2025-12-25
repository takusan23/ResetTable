package io.github.takusan23.resettable.tool

import io.github.takusan23.resettable.tool.ResetTableTool.verifyResultItemRecipe
import io.github.takusan23.resettable.tool.data.RecipeResolveData
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.CraftingRecipe
import net.minecraft.world.item.crafting.ShapedRecipe
import net.minecraft.world.item.crafting.display.SlotDisplayContext
import net.minecraft.world.item.enchantment.EnchantmentHelper

/** このMODの目的となる作ったアイテムを戻すための関数がある */
object ResetTableTool {

    /** 赤色カラーコード */
    private val COLOR_RED = DyeColor.RED.textColor

    /** 青色カラーコード */
    private val COLOR_BLUE = DyeColor.BLUE.textColor

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

    /** craft メソッド、多分定形、不定形レシピ以外は null で呼び出せない。ので try-catch */
    private fun CraftingRecipe.craftOrNull(level: ServerLevel): ItemStack? = runCatching {
        assemble(CraftingInput.EMPTY, level.registryAccess())
    }.getOrNull()

    /**
     * レシピを探して返す。アイテム数があってるかとかは見ていない
     *
     * @param world レシピもらうのに使う
     * @param resetItemStack 探すアイテム
     * @return レシピの配列
     */
    private fun findRecipe(world: ServerLevel, resetItemStack: ItemStack): List<CraftingRecipe> {
        val recipeManager = world.recipeAccess().recipes
        return recipeManager
            // ID と Recipe の Map になってる、Recipe だけにする
            .map { it.value }
            // 作業台だけ
            .filterIsInstance<CraftingRecipe>()
            // クラフトレシピを完成品から探す
            .filter { it.craftOrNull(world)?.item == resetItemStack.item }
    }

    /**
     * 引数のアイテムがちゃんと戻せるか確認する関数
     *
     * @param serverWorld レシピを取得するのに使う。サーバー側しかレシピにアクセスできなくなった。。。
     * @param resultItemStack 検証するアイテム
     * @return [VerifyResult]
     */
    fun verifyResultItemRecipe(serverWorld: ServerLevel, resultItemStack: ItemStack): VerifyResult {
        val recipeList = findRecipe(serverWorld, resultItemStack)
        val availableRecipe = recipeList.firstOrNull {
            val craftRecipeCount = it.craftOrNull(serverWorld)?.count
            if (craftRecipeCount != null) craftRecipeCount <= resultItemStack.count else false
        }

        // TODO NBT のチェックをする場合
        // if (!resultItemStack.isEmpty) {
        //     (world as? ServerWorld)?.server?.registryManager?.also { registryManager ->
        //         val a = resultItemStack.encode(registryManager)
        //         println("NbtElement = $a")
        //     }
        // }

        // 元のアイテムと比較して、何かしらデータコンポーネント（NBT）が付与されている場合は true
        // エンチャント済みとか、シュルカーボックスの中身が入っているとか。元のアイテムからデータがある場合はダメ
        val hasDiffOriginItem = !ItemStack.isSameItemSameComponents(resultItemStack, ItemStack(resultItemStack.item))

        return when {
            resultItemStack.isEmpty -> VerifyResult.ERROR_EMPTY_ITEM_STACK
            recipeList.isEmpty() -> VerifyResult.ERROR_NOT_FOUND_RECIPE
            resultItemStack.isDamaged -> VerifyResult.ERROR_ITEM_DAMAGED
            !EnchantmentHelper.getEnchantmentsForCrafting(resultItemStack).isEmpty -> VerifyResult.ERROR_ENCHANTED_ITEM
            availableRecipe == null -> VerifyResult.ERROR_REQUIRE_STACK_COUNT
            hasDiffOriginItem -> VerifyResult.ERROR_HAS_METADATA
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
    fun findCraftingMaterial(world: ServerLevel, resetItemStack: ItemStack): List<RecipeResolveData>? {
        // 検証した結果もとに戻せない場合はnullを返す
        if (verifyResultItemRecipe(world, resetItemStack) != VerifyResult.SUCCESS) return null

        // クラフトレシピを完成品から探す
        val recipeList = findRecipe(world, resetItemStack)
            // スタック数を確認する
            // 同じ完成品のレシピで複数返す場合に備えて
            .filter {
                val resultItem = it.craftOrNull(world)
                if (resultItem != null) resultItem.count <= resetItemStack.count else false
            }

        val createParameters = SlotDisplayContext.fromLevel(world)
        val recipeResolvedDataList = recipeList.map { recipe ->
            val resetItemStackCount = resetItemStack.count
            val recipeCreateItemCount = recipe.craftOrNull(world)?.count ?: 0
            // 0で割ることがあるらしい
            if (resetItemStackCount >= 1 && recipeCreateItemCount >= 1) {
                // 割り算して何個戻せるか
                val craftCount = resetItemStackCount / recipeCreateItemCount
                // 戻したけど余ったぶん
                val notResolveCount = resetItemStackCount % recipeCreateItemCount
                // 返す
                val notResolveItemStack = resetItemStack.copy().apply { count = notResolveCount }

                // 定形レシピの場合は材料スロット(3x3)で正しいアイテムの配列に置き換える
                if (recipe is ShapedRecipe) {
                    val ingredientPlacement = recipe.placementInfo()
                    // placementSlots に数字か null が入ってて、数字の場合は placements 配列のインデックスとして使えば良い。
                    // null は empty
                    val shapedRecipeList = ingredientPlacement.slotsToIngredientIndex().map { placerOutputPositionOrNegative ->
                        // ingredients でのインデックス
                        if (placerOutputPositionOrNegative == -1) {
                            // 空のスロットの場合は -1
                            ItemStack.EMPTY
                        } else {
                            // findFirst() している。例えばチェストとかはオークの木材以外でも作れるので getStacks() には木の種類が入ってる
                            ingredientPlacement.ingredients()[placerOutputPositionOrNegative].display().resolveForStacks(createParameters)
                                .first()
                                .apply { count = craftCount }
                        }
                    }

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
                    repeat(patternHeight) {
                        // ここで各横スロットのアイテムを一斉に入れている
                        // prevPosには各横スロットの最後のIndexが入ってる
                        recipePatternList.addAll(shapedRecipeList.subList(prevPos, prevPos + patternWidth))
                        // 幅が3未満の場合は残りを空のアイテムで埋める
                        repeat(3 - patternWidth) {
                            recipePatternList.add(ItemStack.EMPTY)
                        }
                        prevPos += patternWidth
                    }
                    RecipeResolveData(recipePatternList, notResolveItemStack)
                } else {
                    val materialList = recipe.placementInfo().ingredients().map { ingredient ->
                        ingredient.display().resolveForStacks(createParameters)
                            .first()
                            .apply { count = craftCount }
                    }
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
        return Component.translatable(result.localizeKey).string to result.textColor
    }

}