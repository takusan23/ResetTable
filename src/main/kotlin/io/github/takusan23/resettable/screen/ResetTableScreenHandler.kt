package io.github.takusan23.resettable.screen

import io.github.takusan23.resettable.entity.ResetTableEntity
import io.github.takusan23.resettable.entity.ResetTableEntity.Companion.RESET_TABLE_RESET_ITEM_SLOT
import io.github.takusan23.resettable.entity.ResetTableScreenHandlers
import io.github.takusan23.resettable.tool.ResetTableTool
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot

/**
 * クライアントとサーバーでGUIの状態を同期させるのに必要なクラス
 * */
class ResetTableScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory,
    private val inventory: Inventory = SimpleInventory(10),
) : ScreenHandler(ResetTableScreenHandlers.RESET_TABLE_SCREEN_HANDLER, syncId) {
    private val player = playerInventory.player
    private val world = player.world

    init {
        // インベントリのGUIを開く
        inventory.onOpen(playerInventory.player)

        // インベントリの変更コールバックを作ったので購読する
        (inventory as? ResetTableEntity)?.addChangeListener {
            // 作るのに必要な材料を返す
            val recipeResolveData = ResetTableTool.findCraftingMaterial(world, getResetItemStack())
            // 一度だけ。もとに戻したアイテムが入るスロットがからじゃない場合も受け付けない
            if (recipeResolveData != null && isMaterialSlotEmpty()) {
                recipeResolveData.resultItemStack.forEachIndexed { index, itemStack -> inventory.setStack(index, itemStack) }
                // 戻したので完成品スロットをクリアするか、戻しきれなかったアイテムを入れる
                inventory.setStack(RESET_TABLE_RESET_ITEM_SLOT, recipeResolveData.resolveSlotItemStack)
            }
        }

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
     * スロットに入れたアイテムが戻せるか確かめる関数
     *
     * @return [ResetTableTool.VerifyResult]
     * */
    fun verifyResultItem(): ResetTableTool.VerifyResult {
        return ResetTableTool.verifyResultItemRecipe(world, getResetItemStack())
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
     * 完成品スロットのアイテムを取得する
     *
     * @return 完成品スロットにあるアイテム
     * */
    private fun getResetItemStack(): ItemStack {
        return inventory.getStack(RESET_TABLE_RESET_ITEM_SLOT)
    }

    /**
     * 戻したアイテムが入るスロットが空っぽかどうか
     *
     * @return 3x3 のスロットが空っぽならtrue
     * */
    private fun isMaterialSlotEmpty(): Boolean {
        return (0..8).map { inventory.getStack(it) }.all { it.isEmpty }
    }
}