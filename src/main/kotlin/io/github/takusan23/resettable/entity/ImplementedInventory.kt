package io.github.takusan23.resettable.entity

import net.minecraft.core.NonNullList
import net.minecraft.world.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack


/**
 * 簡単な[Inventory]の実装
 *
 * パクった：[https://fabricmc.net/wiki/tutorial:inventory]
 * */
fun interface ImplementedInventory : Container {

    companion object {
        /**
         * アイテムリストからインベントリを作成する
         *
         * @param items アイテムスタックの配列
         * */
        fun from(items: NonNullList<ItemStack>): ImplementedInventory {
            // SAM変換
            return ImplementedInventory { items }
        }

        /**
         * サイズを指定してインベントリを作成する
         *
         * @param size 大きさ
         * */
        fun fromSize(size: Int): ImplementedInventory {
            return from(NonNullList.withSize(size, ItemStack.EMPTY))
        }
    }

    /**
     * インベントリのアイテムリストを返す。
     *
     * 呼び出されるたびに同じインスタンスを返してね。
     * */
    fun getItems(): NonNullList<ItemStack>


    /**
     * インベントリの大きさを返す
     * */
    override fun getContainerSize(): Int {
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
    override fun getItem(slot: Int): ItemStack {
        return getItems()[slot]
    }

    /**
     * インベントリからアイテムスタックを削除します
     *
     * @param i 削除するスロット
     * @param j 削除するアイテム数
     * @return 削除したアイテムスタック
     */
    override fun removeItem(i: Int, j: Int): ItemStack {
        val result = ContainerHelper.removeItem(getItems(), i, j)
        if (!result.isEmpty) {
            setChanged()
        }
        return result
    }

    /**
     * 指定したスロットのアイテムを削除します
     *
     * @param i 削除するスロット
     * @return 削除したアイテム
     */
    override fun removeItemNoUpdate(i: Int): ItemStack {
        return ContainerHelper.takeItem(getItems(), i)
    }

    /**
     * インベントリスロットの現在のスタックを置き換える
     *
     * @param slot 位置
     * @param stack 置き換えるアイテムスタック
     */
    override fun setItem(slot: Int, stack: ItemStack) {
        getItems()[slot] = stack
        if (stack.count > maxStackSize) {
            stack.count = maxStackSize
        }
    }

    /**
     * クリアする
     * */
    override fun clearContent() {
        getItems().clear()
    }

    /**
     * 指定したプレイヤーがエンティティにアクセスできるか
     *
     * @return アクセスできる場合はtrue
     * */
    override fun stillValid(player: Player): Boolean {
        return true
    }

    /**
     * 継承して使って
     * */
    override fun setChanged() {

    }

}