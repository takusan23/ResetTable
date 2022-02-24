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
import kotlin.math.min

/**
 * クライアントとサーバーでGUIの状態を同期させるのに必要なクラス
 *
 * @param propertyDelegate 整数値？を[ResetTableEntity]とレシピのページ番号を同期させるので使う
 * @param inventory [ResetTableEntity]にあるやつを渡して
 * */
class ResetTableScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory,
    private val inventory: Inventory = SimpleInventory(10),
    val propertyDelegate: PropertyDelegate = ArrayPropertyDelegate(ResetTableEntity.DelegatePropertyKeys.getPropertyKeySize()),
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

    /**
     * GUIでボタンを押したときに呼ばれる
     *
     * @param id 今回は[ResetTableScreen.RECIPE_MORE_NEXT_BUTTON_ID]とか
     * */
    override fun onButtonClick(player: PlayerEntity?, id: Int): Boolean {
        val currentValue = propertyDelegate.get(ResetTableEntity.DelegatePropertyKeys.PAGE_INDEX.index)
        val pageCount = getRecipePatternCount() ?: 0
        val pageIndex = when (id) {
            RECIPE_MORE_NEXT_BUTTON_ID -> currentValue + 1
            RECIPE_MORE_PREV_BUTTON_ID -> currentValue - 1
            else -> return false
        }
        // 範囲外対策
        if (pageIndex in 0 until pageCount) {
            propertyDelegate.set(ResetTableEntity.DelegatePropertyKeys.PAGE_INDEX.index, min(pageIndex, getRecipePatternCount() ?: 0))
        }
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
     * レシピのパターンが何種類あるか返す
     *
     * @return パターン数。レシピが解決できない場合はnull
     * */
    fun getRecipePatternCount(): Int? {
        val world = playerInventory.player.world
        return ResetTableTool.findCraftingMaterial(world, getResetItemStack())?.size
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