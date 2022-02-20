package io.github.takusan23.resettable.entity

import io.github.takusan23.resettable.screen.ResetTableScreenHandler
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

    /** 登録したコールバックを入れておく配列 */
    private val onChangeListenerList: MutableList<() -> Unit> = mutableListOf()

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
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST,
            -> intArrayOf()
            null -> intArrayOf()
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
     * ここでレシピ判定すれば良さそう？
     * */
    override fun markDirty() {
        // コールバックを呼ぶ
        onChangeListenerList.forEach { it.invoke() }
    }

    /**
     * アイテム変更イベント？を購読する
     *
     * @param update アイテムの操作があったら呼ばれる関数
     * */
    fun addChangeListener(update: () -> Unit) {
        onChangeListenerList.add(update)
    }

    companion object {

        /** リセットしたいアイテムが入るスロット番号 */
        const val RESET_TABLE_RESET_ITEM_SLOT = 9

    }
}