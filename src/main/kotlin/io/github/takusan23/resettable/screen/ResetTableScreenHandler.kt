package io.github.takusan23.resettable.screen

import io.github.takusan23.resettable.entity.ResetTableEntity
import io.github.takusan23.resettable.entity.ResetTableEntity.Companion.RESET_TABLE_RESET_ITEM_SLOT
import io.github.takusan23.resettable.network.ResetTableErrorPayload
import io.github.takusan23.resettable.tool.ResetTableTool
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

/**
 * クライアントとサーバーでGUIの状態を同期させるのに必要なクラス
 *
 * @param inventory [ResetTableEntity]にあるやつを渡して
 * */
class ResetTableScreenHandler(
    syncId: Int,
    private val playerInventory: Inventory,
    private val inventory: Container = SimpleContainer(10)
) : AbstractContainerMenu(ResetTableScreenHandlers.RESET_TABLE_SCREEN_HANDLER, syncId) {

    /** 開いてるGUIがあるEntityのブロックの位置 */
    private var blockPos = BlockPos.ZERO!!

    /**
     * アイテムが戻せない理由をセットする。
     * 本当は GUI 側（クライアント側）でもレシピを検索すれば良いのだが、クライアントでレシピにアクセスできなくなった。
     * そのためサーバーから[ResetTableErrorPayload]を介して教えて貰う必要がある。
     */
    var recipeVerifyResult: ResetTableTool.VerifyResult = ResetTableTool.VerifyResult.ERROR_EMPTY_ITEM_STACK

    /**
     * クライアント側で呼ばれるコンストラクター
     *
     * @param serverClientData サーバーから送られてくる値
     */
    constructor(syncId: Int, playerInventory: Inventory, serverClientData: ResetTableScreenHandlerServerClientData) : this(syncId, playerInventory) {
        blockPos = serverClientData.blockPos
    }

    init {
        // インベントリのGUIを開く
        inventory.startOpen(playerInventory.player)

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

    /** 多分シフトキー押したときの挙動 */
    override fun quickMoveStack(player: Player, i: Int): ItemStack {
        var newStack = ItemStack.EMPTY
        val slot = slots[i]
        if (slot.hasItem()) {
            val originalStack = slot.item
            newStack = originalStack.copy()
            if (i < inventory.containerSize) {
                // ResetTableのインベントリ -> プレイヤーのインベントリ
                if (!moveItemStackTo(originalStack, inventory.containerSize, slots.size, true)) {
                    return ItemStack.EMPTY
                }
            } else {
                // プレイヤーのインベントリ -> ResetTableのインベントリ
                // 材料スロット（3x3）の領域には入れたくないので0番目だけ入れるように
                if (!moveItemStackTo(originalStack, 0, 1, false)) {
                    return ItemStack.EMPTY
                }
            }
            if (originalStack.isEmpty) {
                slot.setByPlayer(ItemStack.EMPTY)
            } else {
                slot.setChanged()
            }
        }
        // 何も無いスロットでシフトクリックしても呼ばれてしまうので一応制御
        if (!newStack.isEmpty) {
            // 戻せない場合は理由を送信
            sendVerifyResultToClient(player)
        }
        return newStack
    }

    /** よくわからｎ */
    override fun stillValid(player: Player): Boolean {
        return this.inventory.stillValid(player)
    }

    /** イベントリのスロットを押したとき */
    override fun clicked(i: Int, j: Int, clickType: ClickType, player: Player) {
        super.clicked(i, j, clickType, player)
        // もとに戻したいアイテムのスロットの時のみ
        if (i != SLOT_RESET_INDEX) return
        // 戻せない場合は理由を送信
        sendVerifyResultToClient(player)
    }

    /** アイテムをもとに戻せない理由をクライアント側へ送る */
    private fun sendVerifyResultToClient(player: Player?) {
        // サーバー側であること
        if (player !is ServerPlayer) return

        // 戻せない理由をクライアント側（GUI）へ送る
        // 実際に元に戻す処理は ResetTableEntity の markDirty 関数を見てください。
        // なんで戻せない理由をここで判断しているかというと、クライアント側へ送る際に PlayerEntity が必要そうで、markDirty には無い。
        // ちなみに getResetItemStack() が空の場合はそれ用のエラーになりますが、GUI 側で表示しないようにしているので、特に分岐せずクライアント側へ送ります。
        val verifyResult = ResetTableTool.verifyResultItemRecipe(
            serverWorld = player.level(),
            resultItemStack = getResetItemStack()
        )
        ServerPlayNetworking.send(player, ResetTableErrorPayload(blockPos, verifyResult))
    }

    /**
     * レシピのパターンが何種類あるか返す
     *
     * @return パターン数。レシピが解決できない場合はnull
     */
    fun getRecipePatternCount(): Int? {
        val serverWorld = (playerInventory.player as? ServerPlayer)?.level() ?: return null
        return ResetTableTool.findCraftingMaterial(serverWorld, getResetItemStack())?.size
    }

    /**
     * 完成品スロットのアイテムを取得する
     *
     * @return 完成品スロットにあるアイテム
     */
    private fun getResetItemStack(): ItemStack {
        return inventory.getItem(RESET_TABLE_RESET_ITEM_SLOT)
    }

    companion object {
        /** もとに戻したいアイテムのスロットのインデックス */
        private const val SLOT_RESET_INDEX = 0
    }
}