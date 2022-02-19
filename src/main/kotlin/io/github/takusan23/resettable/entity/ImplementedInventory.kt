package io.github.takusan23.resettable.entity

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.util.collection.DefaultedList


/**
 * 簡単な[Inventory]の実装
 *
 * パクった：https://fabricmc.net/wiki/tutorial:inventory
 * */
fun interface ImplementedInventory : Inventory {

    companion object {
        /**
         * アイテムリストからインベントリを作成する
         *
         * @param items アイテムスタックの配列
         * */
        fun from(items: DefaultedList<ItemStack>): ImplementedInventory {
            // SAM変換
            return ImplementedInventory { items }
        }

        /**
         * サイズを指定してインベントリを作成する
         *
         * @param size 大きさ
         * */
        fun fromSize(size: Int): ImplementedInventory {
            return from(DefaultedList.ofSize(size, ItemStack.EMPTY))
        }
    }

    /**
     * インベントリのアイテムリストを返す。
     *
     * 呼び出されるたびに同じインスタンスを返してね。
     * */
    fun getItems(): DefaultedList<ItemStack>


    /**
     * インベントリの大きさを返す
     * */
    override fun size(): Int {
        return getItems().size
    }

    /**
     * インベントリが空かどうか返します
     *
     * @return 空のスタックしかない場合はtrue
     */
    override fun isEmpty(): Boolean {
        // 配列操作用関数好き
        return getItems().all { it.isEmpty }
    }

    /**
     * 指定したスロットのアイテムスタックを返します
     *
     * @param slot 位置
     * @return 位置のアイテムスタック
     */
    override fun getStack(slot: Int): ItemStack {
        return getItems()[slot]
    }

    /**
     * インベントリからアイテムスタックを削除します
     *
     * @param slot  削除するスロット
     * @param count 削除するアイテム数
     * @return 削除したアイテムスタック
     */
    override fun removeStack(slot: Int, count: Int): ItemStack? {
        val result = Inventories.splitStack(getItems(), slot, count)
        if (!result.isEmpty) {
            markDirty()
        }
        return result
    }

    /**
     * 指定したスロットのアイテムを削除します
     *
     * @param slot 削除するスロット
     * @return 削除したアイテム
     */
    override fun removeStack(slot: Int): ItemStack? {
        return Inventories.removeStack(getItems(), slot)
    }

    /**
     * インベントリスロットの現在のスタックを置き換える
     *
     * @param slot 位置
     * @param stack 置き換えるアイテムスタック
     */
    override fun setStack(slot: Int, stack: ItemStack) {
        getItems()[slot] = stack
        if (stack.count > maxCountPerStack) {
            stack.count = maxCountPerStack
        }
    }

    /**
     * クリアする
     * */
    override fun clear() {
        getItems().clear()
    }

    /**
     * 指定したプレイヤーがエンティティにアクセスできるか
     *
     * @return アクセスできる場合はtrue
     * */
    override fun canPlayerUse(player: PlayerEntity?): Boolean {
        return true
    }

    /**
     * 継承して使って
     * */
    override fun markDirty() {

    }

}