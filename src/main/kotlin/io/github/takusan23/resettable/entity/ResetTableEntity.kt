package io.github.takusan23.resettable.entity

import io.github.takusan23.resettable.screen.ResetTableScreenHandler
import io.github.takusan23.resettable.tool.ResetTableTool
import io.github.takusan23.resettable.tool.data.RecipeResolveData
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
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
) : BlockEntity(ResetTableEntities.RESET_TABLE_BLOCK_ENTITY, pos, state), NamedScreenHandlerFactory, ExtendedScreenHandlerFactory, ImplementedInventory, SidedInventory {

    /**
     * リセットテーブルのインベントリ
     *
     * 3x3 の分と還元スロットのために +1
     * */
    private val inventory = DefaultedList.ofSize(10, ItemStack.EMPTY)

    /** アイテム変更コールバックの配列 */
    private val itemChangeCallbackList = mutableListOf<() -> Unit>()

    /** 還元スロットに入れたアイテムのレシピ検索結果 */
    private var currentRecipeResolveDataList: List<RecipeResolveData>? = null

    private val prevMaterialSlotItemList = mutableListOf<ItemStack>()

    /** 還元スロットに入っているアイテム */
    var currentResetSlotItemStack = ItemStack.EMPTY
        private set

    /** 複数あった場合にレシピを切り替えるための */
    private var pageIndex = 0

    /** [ResetTableScreenHandler]と[ResetTableEntity]の中で[pageIndex]を同期させる */
    private val propertyDelegate = object : PropertyDelegate {
        override fun get(index: Int): Int {
            return when (index) {
                DelegatePropertyKeys.PAGE_INDEX.index -> pageIndex
                else -> 0
            }
        }

        override fun set(index: Int, value: Int) {
            when (index) {
                DelegatePropertyKeys.PAGE_INDEX.index -> pageIndex = value
            }
        }

        override fun size(): Int {
            return DelegatePropertyKeys.getPropertyKeySize()
        }
    }

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
        return ResetTableScreenHandler(syncId, playerInventory, this, propertyDelegate)
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
     * サーバー側で呼ばれる。
     *
     * クライアントに贈りたいデータをここで詰めておく。
     * */
    override fun writeScreenOpeningData(player: ServerPlayerEntity?, buf: PacketByteBuf?) {
        buf?.apply {
            // クライアント側（GUI）でブロックの位置を知りたいので渡しておく
            writeBlockPos(pos)
        }
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

    /** いまの還元スロットに入っているアイテムのレシピを探して、材料スロット（3x3）に入れる */
    private fun updateResult() {

        val resetSlotItemStack = getStack(RESET_TABLE_RESET_ITEM_SLOT)
        currentResetSlotItemStack = resetSlotItemStack

        updateResultItems()

        prevMaterialSlotItemList.clear()
        prevMaterialSlotItemList.addAll(getMaterialSlotItemStackList())

/*
        // 作るのに必要なアイテムを取得する
        // 還元スロットのアイテムが変わっていたら再計算
        if (ItemStack.areItemsEqual(currentResetSlotItemStack, resetSlotItemStack)) {
            currentRecipeResolveDataList = ResetTableTool.findCraftingMaterial(nonNullWorld, resetSlotItemStack) ?: return
        }

        // 材料スロットが空いていれば
        if (isMaterialSlotEmpty()) {
            updateResultItems()
        }
*/


    }

    private fun updateResultItems() {
        val world = world ?: return
        currentRecipeResolveDataList = ResetTableTool.findCraftingMaterial(world, currentResetSlotItemStack)
        if (isMaterialSlotEmpty()) {
            currentRecipeResolveDataList
                ?.getOrNull(0)
                ?.also { recipeResolveData ->
                    recipeResolveData
                        .recipePatternFormattedList
                        .forEachIndexed { index, itemStack -> setStack(index, itemStack) }
                    recipeResolveData
                        .resolveSlotItemStack
                        .also { setStack(RESET_TABLE_RESET_ITEM_SLOT, it) }
                }
        } else {
            // スロット空いてないけど、今のスロットと同じ中身だった場合
            currentRecipeResolveDataList
                ?.firstOrNull { recipeResolveData ->
                    isEqualItemByItemStackList(getMaterialSlotItemStackList(), recipeResolveData.recipePatternFormattedList)
                }?.also { recipeResolveData ->
                    // アイテム数を増やす
                    getMaterialSlotItemStackList()
                        .map { it.copy().apply { count += recipeResolveData.recipePatternFormattedList[0].count } }
                        .forEachIndexed { index, itemStack -> setStack(index, itemStack) }
                    // 割り切れなかったアイテムを還元スロットへ
                    setStack(RESET_TABLE_RESET_ITEM_SLOT, recipeResolveData.resolveSlotItemStack)
                }
        }
    }

    /**
     * アイテム変更コールバックを登録する。使ってないけどいつか使うかも
     *
     * @param callback アイテム変更時に呼ばれる関数
     * */
    @Suppress("unused")
    fun addItemChangeCallback(callback: () -> Unit) {
        itemChangeCallbackList.add(callback)
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
    private fun getMaterialSlotItemStackList(): List<ItemStack> {
        return (0..8).map { getStack(it) }
    }

    /**
     * 2つのItemStack配列を見て、同じアイテムが入っている場合はtrue。スタック数等は見ていない
     *
     * @param list1 ItemStackの配列
     * @param list2 ItemStackの配列
     * @return 同じ場合はtrue
     * */
    private fun isEqualItemByItemStackList(list1: List<ItemStack>, list2: List<ItemStack>): Boolean {
        return (0..kotlin.math.max(list1.size, list2.size)).map { index ->
            val list1Item = list1.getOrNull(index) ?: ItemStack.EMPTY
            val list2Item = list2.getOrNull(index) ?: ItemStack.EMPTY
            list1Item.item == list2Item.item
        }.all { it }
    }

    /**
     * 現在のレシピ番号を返す
     *
     * @return レシピ番号
     * */
    private fun getRecipePageIndex(): Int {
        return propertyDelegate.get(DelegatePropertyKeys.PAGE_INDEX.index)
    }

    /**
     * [PropertyDelegate]で使うキー代わり。[index]をキー代わりにする
     * */
    enum class DelegatePropertyKeys {
        /** ページ切り替え番号 */
        PAGE_INDEX;

        /** 整数値を返す */
        val index: Int
            get() = this.ordinal

        companion object {
            /** [DelegatePropertyKeys]が何個あるか返す */
            fun getPropertyKeySize(): Int {
                return values().size
            }
        }
    }

    companion object {

        /** リセットしたいアイテムが入るスロット番号 */
        const val RESET_TABLE_RESET_ITEM_SLOT = 9
    }
}