package io.github.takusan23.resettable

import io.github.takusan23.resettable.network.ResetTableErrorPayload
import io.github.takusan23.resettable.screen.ResetTableScreen
import io.github.takusan23.resettable.screen.ResetTableScreenHandler
import io.github.takusan23.resettable.screen.ResetTableScreenHandlers
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.screen.ingame.HandledScreens
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier


/**
 * GUI関係はクライアント側のみ初期化するので
 *
 * fabric.mod.json で client の指定必須
 */
@Suppress("unused")
fun clientSideInit() {

    // クライアント側のみGUIの画面をFabricに登録する
    Registry.register(Registries.SCREEN_HANDLER, Identifier.of("resettable", "reset_table_block"), ResetTableScreenHandlers.RESET_TABLE_SCREEN_HANDLER)
    HandledScreens.register(ResetTableScreenHandlers.RESET_TABLE_SCREEN_HANDLER) { handler, inventory, title ->
        ResetTableScreen(handler, inventory, title)
    }

    // クライアント側は追加でネットワーク登録が必要
    ClientPlayNetworking.registerGlobalReceiver(ResetTableErrorPayload.ID) { payload, context ->
        context.client().execute {
            // ネットワーク経由でイベントが来た
            // 今表示されている画面がリセットテーブルの GUI の場合はエラーを出す
            val resetTableScreenHandler = (context.player().currentScreenHandler as? ResetTableScreenHandler)
            if (resetTableScreenHandler != null) {
                resetTableScreenHandler.recipeVerifyResult = payload.verifyResult
            }
        }
    }

}