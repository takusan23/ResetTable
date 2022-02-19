package io.github.takusan23.resettable.screen

import io.github.takusan23.resettable.entity.ResetTableScreenHandlers
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
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

    init {
        // インベントリのGUIを開く
        inventory.onOpen(playerInventory.player)
        addSlot(Slot(inventory, 9, 124, 35))

        // リセットテーブルの 3x3 の スロット
        repeat(3) { i ->
            repeat(3) { j ->
                this.addSlot(Slot(inventory, j + i * 3, 30 + j * 18, 17 + i * 18))
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

    override fun canUse(player: PlayerEntity?): Boolean {
        return this.inventory.canPlayerUse(player);
    }
}