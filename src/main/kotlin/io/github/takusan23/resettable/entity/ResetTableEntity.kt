package io.github.takusan23.resettable.entity

import io.github.takusan23.resettable.screen.ResetTableScreenHandler
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos

/**
 * リセットテーブルが持つEntity（アイテム保持）
 *
 * @param pos [ResetTableEntities] 参照
 * @param state [ResetTableEntities] 参照
 * */
class ResetTableEntity(
    pos: BlockPos,
    state: BlockState,
) : BlockEntity(ResetTableEntities.RESET_TABLE_BLOCK_ENTITY, pos, state), NamedScreenHandlerFactory, ImplementedInventory {
    /** リセットテーブルのインベントリ */
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

    /**
     * 多分アイテムを入れたりしたときに呼ばれる
     *
     * ここでレシピ判定すれば良さそう？
     * */
    override fun markDirty() {
        // コールバックを呼ぶ
        onChangeListenerList.forEach { it.invoke() }
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
     * アイテム変更イベント？を購読する
     *
     * @param update アイテムの操作があったら呼ばれる関数
     * */
    fun addChangeListener(update: () -> Unit) {
        onChangeListenerList.add(update)
    }
}