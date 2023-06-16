package io.github.takusan23.resettable.screen

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType

/**
 * このMODで使うスクリーンハンドラー
 * */
object ResetTableScreenHandlers {

    /**
     * リセットテーブルブロックのエンティティのスクリーンハンドラー
     */
    val RESET_TABLE_SCREEN_HANDLER = ExtendedScreenHandlerType { syncId, inventory, buf -> ResetTableScreenHandler(syncId, inventory, buf) }

}