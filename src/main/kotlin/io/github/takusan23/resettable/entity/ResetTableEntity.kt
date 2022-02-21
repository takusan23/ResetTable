package io.github.takusan23.resettable.entity

import io.github.takusan23.resettable.screen.ResetTableScreenHandler
import io.github.takusan23.resettable.tool.ResetTableTool
import io.github.takusan23.resettable.tool.data.RecipeResolveData
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

/**
 * リセットテーブルが持つEntity
 *
 * 内部でアイテムを保持しておくクラス。還元スロットは普通の作業台で言うところの完成品スロットの部分です。indexだと9
 *
 * [SidedInventory]を継承したのでホッパーで自動化できる・・・？
 *
 * @param pos [ResetTableEntities] 参照
 * @param state [ResetTableEntities] 参照
 * */
class ResetTableEntity(
    pos: BlockPos,
    state: BlockState,
) : BlockEntity(ResetTableEntities.RESET_TABLE_BLOCK_ENTITY, pos, state), NamedScreenHandlerFactory, ImplementedInventory, SidedInventory {

    /**
     * リセットテーブルのインベントリ
     *
     * 3x3 の分と還元スロットのために +1
     * */
    private val inventory = DefaultedList.ofSize(10, ItemStack.EMPTY)

    /** アイテム変更コールバックの配列 */
    private val itemChangeCallbackList = mutableListOf<() -> Unit>()

    /**
     * Entityが持っているアイテムを返す
     *
     * @return 保持しているアイテム
     * */
    override fun getItems(): DefaultedList<ItemStack> {
        return inventory
    }

    /** GUIを返す？ */
    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler {
        return ResetTableScreenHandler(syncId, playerInventory, this)
    }

    /** ホッパー等からアクセスできるスロットを返す */
    override fun getAvailableSlots(side: Direction?): IntArray {
        return when (side) {
            Direction.UP -> intArrayOf(RESET_TABLE_RESET_ITEM_SLOT)
            Direction.DOWN -> (0..8).toList().toIntArray()
            else -> intArrayOf()
        }
    }

    /** アイテムをホッパー等から受け付けるか */
    override fun canInsert(slot: Int, stack: ItemStack?, dir: Direction?): Boolean {
        // ItemStackが引数で貰えますが、常に count=1 で足りているかわからないため、
        // いま還元スロットに入っているアイテムと同じ場合と空っぽの場合に受け付けます
        val resetSlotItemStack = getStack(RESET_TABLE_RESET_ITEM_SLOT)
        return resetSlotItemStack.isEmpty || resetSlotItemStack.isOf(stack?.item)
    }

    /** アイテムを取り出せるか */
    override fun canExtract(slot: Int, stack: ItemStack?, dir: Direction?): Boolean {
        // 3x3 の範囲内ならok
        return slot in 0..8
    }

    /** インベントリを保存する */
    override fun writeNbt(nbt: NbtCompound?) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, this.inventory)
    }

    /** 保存したインベントリを取り出す */
    override fun readNbt(nbt: NbtCompound?) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, this.inventory)
    }

    override fun getDisplayName(): Text {
        // ブロックのローカライズテキストをそのまま利用する
        return TranslatableText(cachedState.block.translationKey)
    }

    /**
     * 多分アイテムを入れたりしたときに呼ばれる
     *
     * ここでレシピ検索をしている
     * */
    override fun markDirty() {
        updateResult()
        itemChangeCallbackList.forEach { it.invoke() }
    }

    /** 還元スロットに入れたアイテムのレシピ検索結果 */
    private var currentRecipeResolveData: RecipeResolveData? = null

    /** 複数あった場合にレシピを切り替えるための */
    private var pageIndex = 0

    /** 還元スロットに入っているアイテム */
    private var currentResetSlotItemStack = ItemStack.EMPTY

    /** いまの還元スロットに入っているアイテムのレシピを探して、材料スロット（3x3）に入れる */
    private fun updateResult() {
        val nonNullWorld = world ?: return
        val resetSlotItemStack = getStack(RESET_TABLE_RESET_ITEM_SLOT)

        // 作るのに必要なアイテムを取得する
        // 還元スロットのアイテムが変わっていたら再計算
        if (ItemStack.areItemsEqual(currentResetSlotItemStack, resetSlotItemStack)) {
            currentRecipeResolveData = ResetTableTool.findCraftingMaterial(nonNullWorld, resetSlotItemStack)?.getOrNull(0)
        }

        // 材料スロットが空いていれば
        if (isMaterialSlotEmpty()) {
            setMaterialSlot()
            currentRecipeResolveData?.resolveSlotItemStack?.also { setStack(RESET_TABLE_RESET_ITEM_SLOT, it) }
        }

        currentResetSlotItemStack = resetSlotItemStack

    }

    /** 材料スロットの中身を空っぽにする */
    private fun clearMaterialSlot() {
        (0..8).map { setStack(it, ItemStack.EMPTY) }
    }

    /** [currentRecipeResolveData] を使って材料スロット元に戻す */
    private fun setMaterialSlot() {
        currentRecipeResolveData?.recipePatternFormattedList?.forEachIndexed { index, itemStack ->
            setStack(index, itemStack)
        }
    }

    /**
     * アイテム変更コールバックを登録する。使ってないけどいつか使うかも
     *
     * @param callback アイテム変更時に呼ばれる関数
     * */
    fun addItemChangeCallback(callback: () -> Unit) {
        itemChangeCallbackList.add(callback)
    }

    /**
     * 還元スロットが空っぽならtrue
     *
     * @return 空ならtrue
     * */
    private fun isResetItemSlotEmpty(): Boolean {
        return getStack(RESET_TABLE_RESET_ITEM_SLOT).isEmpty
    }

    /**
     * 戻したアイテムが入るスロットが空っぽかどうか
     *
     * @return 3x3 のスロットが空っぽならtrue
     * */
    private fun isMaterialSlotEmpty(): Boolean {
        return (0..8).map { getStack(it) }.all { it.isEmpty }
    }

    /**
     * 材料スロットのアイテムを取得する
     *
     * @return 3x3 のアイテムスロット
     * */
    private fun getRecipePatternSlotItemList(): List<ItemStack> {
        return (0..8).map { getStack(it) }
    }

    /**
     * ２つのItemStackの配列を見て中身が同じかどうか
     *
     * @param list1 ItemStackの配列
     * @param list2 ItemStackの配列
     * @return 同じ場合はtrue
     * */
    private fun isEqualItemStackList(list1: List<ItemStack>, list2: List<ItemStack>): Boolean {
        return list1.mapIndexed { index, itemStack -> ItemStack.areEqual(list2[index], itemStack) }.all { it }
    }


    companion object {

        /** リセットしたいアイテムが入るスロット番号 */
        const val RESET_TABLE_RESET_ITEM_SLOT = 9

    }
}