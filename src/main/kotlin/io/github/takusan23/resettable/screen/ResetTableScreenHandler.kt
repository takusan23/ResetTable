package io.github.takusan23.resettable.screen

import io.github.takusan23.resettable.entity.ResetTableEntity
import io.github.takusan23.resettable.entity.ResetTableEntity.Companion.RESET_TABLE_RESET_ITEM_SLOT
import io.github.takusan23.resettable.network.ResetTableErrorPayload
import io.github.takusan23.resettable.tool.ResetTableTool
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos

/**
 * クライアントとサーバーでGUIの状態を同期させるのに必要なクラス
 *
 * @param inventory [ResetTableEntity]にあるやつを渡して
 * */
class ResetTableScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory,
    private val inventory: Inventory = SimpleInventory(10)
) : ScreenHandler(ResetTableScreenHandlers.RESET_TABLE_SCREEN_HANDLER, syncId) {

    /** 開いてるGUIがあるEntityのブロックの位置 */
    private var blockPos = BlockPos.ORIGIN!!

    /**
     * アイテムが戻せない理由をセットする。
     * 本当は GUI 側（クライアント側）でもレシピを検索すれば良いのだが、クライアントでレシピにアクセスできなくなった。
     * そのためサーバーから[io.github.takusan23.resettable.network.ResetTableErrorPayload]を介して教えて貰う必要がある。
     */
    var recipeVerifyResult: ResetTableTool.VerifyResult = ResetTableTool.VerifyResult.ERROR_EMPTY_ITEM_STACK

    /**
     * クライアント側で呼ばれるコンストラクター
     *
     * @param serverClientData サーバーから送られてくる値
     */
    constructor(syncId: Int, playerInventory: PlayerInventory, serverClientData: ResetTableScreenHandlerServerClientData) : this(syncId, playerInventory) {
        blockPos = serverClientData.blockPos
    }

    init {
        // インベントリのGUIを開く
        inventory.onOpen(playerInventory.player)

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
    override fun quickMove(player: PlayerEntity?, index: Int): ItemStack {
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
        // 何も無いスロットでシフトクリックしても呼ばれてしまうので一応制御
        if (!newStack.isEmpty) {
            // 戻せない場合は理由を送信
            sendVerifyResultToClient(player)
        }
        return newStack
    }

    /** よくわからｎ */
    override fun canUse(player: PlayerEntity?): Boolean {
        return this.inventory.canPlayerUse(player)
    }

    /** イベントリのスロットを押したとき */
    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType?, player: PlayerEntity?) {
        super.onSlotClick(slotIndex, button, actionType, player)
        // もとに戻したいアイテムのスロットの時のみ
        if (slotIndex != SLOT_RESET_INDEX) return
        // 戻せない場合は理由を送信
        sendVerifyResultToClient(player)
    }

    /** アイテムをもとに戻せない理由をクライアント側へ送る */
    private fun sendVerifyResultToClient(player: PlayerEntity?) {
        // サーバー側であること
        if (player !is ServerPlayerEntity) return

        // 戻せない理由をクライアント側（GUI）へ送る
        // 実際に元に戻す処理は ResetTableEntity の markDirty 関数を見てください。
        // なんで戻せない理由をここで判断しているかというと、クライアント側へ送る際に PlayerEntity が必要そうで、markDirty には無い。
        // ちなみに getResetItemStack() が空の場合はそれ用のエラーになりますが、GUI 側で表示しないようにしているので、特に分岐せずクライアント側へ送ります。
        val verifyResult = ResetTableTool.verifyResultItemRecipe(
            serverWorld = player.serverWorld,
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
        val serverWorld = (playerInventory.player.world as? ServerWorld) ?: return null
        return ResetTableTool.findCraftingMaterial(serverWorld, getResetItemStack())?.size
    }

    /**
     * 完成品スロットのアイテムを取得する
     *
     * @return 完成品スロットにあるアイテム
     */
    private fun getResetItemStack(): ItemStack {
        return inventory.getStack(RESET_TABLE_RESET_ITEM_SLOT)
    }

    companion object {
        /** もとに戻したいアイテムのスロットのインデックス */
        private const val SLOT_RESET_INDEX = 0
    }
}