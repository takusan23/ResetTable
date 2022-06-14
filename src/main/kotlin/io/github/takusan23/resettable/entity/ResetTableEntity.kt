package io.github.takusan23.resettable.entity

import io.github.takusan23.resettable.screen.ResetTableScreenHandler
import io.github.takusan23.resettable.tool.ResetTableTool
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
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
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
        return Text.translatable(cachedState.block.translationKey)
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
        updateResultItems()
        itemChangeCallbackList.forEach { it.invoke() }
    }

    /**
     * いまの還元スロットに入っているアイテムのレシピを探して、材料スロット（3x3）に入れる
     *
     * 既に材料スロットに入っている場合は戻さない、けど前回と同じレシピだった場合は戻す
     * */
    private fun updateResultItems() {
        val world = world ?: return
        val currentResetSlotItemStack = getStack(RESET_TABLE_RESET_ITEM_SLOT)
        val currentRecipeResolveDataList = ResetTableTool.findCraftingMaterial(world, currentResetSlotItemStack)
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
            // ただし材料スロットが1スタックを超えるようなら何もしない
            currentRecipeResolveDataList
                ?.firstOrNull { recipeResolveData ->
                    isEqualItemByItemStackList(getMaterialSlotItemStackList(), recipeResolveData.recipePatternFormattedList)
                            && isInsertableMaterialSlot(recipeResolveData.recipePatternFormattedList[0].count)
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
     * 材料スロットが空っぽかどうか
     *
     * @return 3x3 のスロットが空っぽならtrue
     * */
    private fun isMaterialSlotEmpty(): Boolean {
        return getMaterialSlotItemStackList().all { it.isEmpty }
    }

    /**
     * 材料スロットに指定したアイテム数を入れると、1スタックを超えてしまう場合はfalseを返す
     *
     * @param addCount 追加数
     * @return どれか一つでも1スタックを超える場合はfalse
     * */
    private fun isInsertableMaterialSlot(addCount: Int): Boolean {
        return getMaterialSlotItemStackList().all { it.count + addCount <= ITEM_STACK_MAX_VALUE }
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
        return (0..kotlin.math.max(list1.size, list2.size)).all { index ->
            val list1Item = list1.getOrNull(index) ?: ItemStack.EMPTY
            val list2Item = list2.getOrNull(index) ?: ItemStack.EMPTY
            list1Item.item == list2Item.item
        }
    }

    companion object {

        /** リセットしたいアイテムが入るスロット番号 */
        const val RESET_TABLE_RESET_ITEM_SLOT = 9

        /** 1スタック */
        const val ITEM_STACK_MAX_VALUE = 64
    }
}