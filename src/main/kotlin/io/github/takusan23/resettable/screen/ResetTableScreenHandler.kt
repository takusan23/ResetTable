package io.github.takusan23.resettable.screen

import io.github.takusan23.resettable.entity.ResetTableEntity
import io.github.takusan23.resettable.entity.ResetTableEntity.Companion.RESET_TABLE_RESET_ITEM_SLOT
import io.github.takusan23.resettable.screen.ResetTableScreen.Companion.RECIPE_MORE_NEXT_BUTTON_ID
import io.github.takusan23.resettable.screen.ResetTableScreen.Companion.RECIPE_MORE_PREV_BUTTON_ID
import io.github.takusan23.resettable.tool.ResetTableTool
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.util.math.BlockPos

/**
 * クライアントとサーバーでGUIの状態を同期させるのに必要なクラス
 *
 * @param propertyDelegate 整数値？を[ResetTableEntity]と同期するために必要
 * @param inventory [ResetTableEntity]にあるやつを渡して
 * */
class ResetTableScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory,
    private val inventory: Inventory = SimpleInventory(10),
    private val propertyDelegate: PropertyDelegate = ArrayPropertyDelegate(1),
) : ScreenHandler(ResetTableScreenHandlers.RESET_TABLE_SCREEN_HANDLER, syncId) {

    /** 開いてるGUIがあるEntityのブロックの位置 */
    var blockPos = BlockPos.ORIGIN!!
        private set

    /**
     * クライアント側で呼ばれるコンストラクター
     *
     * 登録の際は [ScreenHandlerRegistry.registerExtended] を使う
     * */
    constructor(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf) : this(syncId, playerInventory) {
        blockPos = buf.readBlockPos()
    }

    init {
        // インベントリのGUIを開く
        inventory.onOpen(playerInventory.player)

        // スクリーンハンドラーにPropertyDelegateが存在しますよ～？って通知しないと同期されない
        addProperties(propertyDelegate)

        // 完成品スロット
        addSlot(Slot(inventory, 9, 124, 35))

        // リセットテーブルの 3x3 のスロット
        repeat(3) { i ->
            repeat(3) { j ->
                addSlot(Slot(inventory, j + i * 3, 30 + j * 18, 17 + i * 18))
            }
        }
        // プレイヤーのインベントリ
        repeat(3) { m ->
            repeat(9) { l ->
                addSlot(Slot(playerInventory, l + m * 9 + 9, 8 + l * 18, 84 + m * 18))
            }
        }
        // プレイヤーのホットバー
        repeat(9) { m ->
            addSlot(Slot(playerInventory, m, 8 + m * 18, 142))
        }
    }

    override fun onButtonClick(player: PlayerEntity?, id: Int): Boolean {
        val pageIndex = when (id) {
            RECIPE_MORE_NEXT_BUTTON_ID -> getRecipePageIndex() + 1
            RECIPE_MORE_PREV_BUTTON_ID -> getRecipePageIndex() - 1
            else -> return false
        }
        setRecipePageIndex(pageIndex)

        (inventory as? ResetTableEntity)?.updateResultItems()

        return true
    }

    /** よくわからｎ */
    override fun canUse(player: PlayerEntity?): Boolean {
        return this.inventory.canPlayerUse(player)
    }

    /** 多分シフトキー押したときの挙動 */
    override fun transferSlot(player: PlayerEntity?, index: Int): ItemStack {
        var newStack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val originalStack = slot.stack
            newStack = originalStack.copy()
            if (index < inventory.size()) {
                // ResetTableのインベントリ -> プレイヤーのインベントリ
                if (!insertItem(originalStack, inventory.size(), slots.size, true)) {
                    return ItemStack.EMPTY
                }
            } else {
                // プレイヤーのインベントリ -> ResetTableのインベントリ
                // 材料スロット（3x3）の領域には入れたくないので0番目だけ入れるように
                if (!insertItem(originalStack, 0, 1, false)) {
                    return ItemStack.EMPTY
                }
            }
            if (originalStack.isEmpty) {
                slot.stack = ItemStack.EMPTY
            } else {
                slot.markDirty()
            }
        }
        return newStack
    }

    /**
     * スロットに入れたアイテムが戻せるか確かめる関数
     *
     * @return [ResetTableTool.VerifyResult]
     * */
    fun verifyResultItem(): ResetTableTool.VerifyResult {
        return ResetTableTool.verifyResultItemRecipe(playerInventory.player.world, getResetItemStack())
    }

    /**
     * ページ切り替え番号を取得する
     *
     * @return 何ページ目か
     * */
    fun getRecipePageIndex(): Int {
        return propertyDelegate.get(0)
    }

    /**
     * ページ切り替え番号を登録する
     *
     * @param index 何ページ目か
     * */
    private fun setRecipePageIndex(index: Int) {
        propertyDelegate.set(0, index)
    }

    /**
     * 完成品スロットのアイテムを取得する
     *
     * @return 完成品スロットにあるアイテム
     * */
    private fun getResetItemStack(): ItemStack {
        return inventory.getStack(RESET_TABLE_RESET_ITEM_SLOT)
    }
}