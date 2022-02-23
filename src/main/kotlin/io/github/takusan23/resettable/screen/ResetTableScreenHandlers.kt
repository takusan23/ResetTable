package io.github.takusan23.resettable.screen

import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry
import net.minecraft.util.Identifier

/**
 * このMODで使うスクリーンハンドラー
 * */
object ResetTableScreenHandlers {

    /**
     * リセットテーブルブロックのエンティティのスクリーンハンドラー
     * */
    val RESET_TABLE_SCREEN_HANDLER = ScreenHandlerRegistry.registerExtended(
        Identifier("resettable", "reset_table_block")
    ) { syncId, inventory, buf -> ResetTableScreenHandler(syncId, inventory, buf) }

}