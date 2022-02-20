package io.github.takusan23.resettable

import io.github.takusan23.resettable.screen.ResetTableScreenHandlers
import io.github.takusan23.resettable.screen.ResetTableScreen
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry

/**
 * GUI関係はクライアント側のみ初期化するので
 *
 * fabric.mod.json で client の指定必須
 * */
@Suppress("unused")
fun clientSideInit() {

    /** クライアント側のみGUIの画面をFabricに登録する */
    ScreenRegistry.register(
        ResetTableScreenHandlers.RESET_TABLE_SCREEN_HANDLER
    ) { handler, inventory, title -> ResetTableScreen(handler, inventory, title) }
}