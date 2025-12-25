package io.github.takusan23.resettable.screen

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec

/**
 * このMODで使うスクリーンハンドラー
 * */
object ResetTableScreenHandlers {

    /**
     * [ResetTableScreenHandlerServerClientData] を [RegistryByteBuf] 経由でシリアライズ・デシリアライズできるやつ。
     * なんか自分で書かないといけなくなった。。。
     */
    private val PACKET_CODEC = StreamCodec.ofMember<RegistryFriendlyByteBuf, ResetTableScreenHandlerServerClientData>(
        /* encoder = */ { data, buf -> ResetTableScreenHandlerServerClientData.write(data, buf) },
        /* decoder = */ { buf -> ResetTableScreenHandlerServerClientData.read(buf) }
    )

    /**
     * リセットテーブルブロックのエンティティのスクリーンハンドラー
     */
    val RESET_TABLE_SCREEN_HANDLER = ExtendedScreenHandlerType(
        { syncId, inventory, serverClientData -> ResetTableScreenHandler(syncId, inventory, serverClientData) },
        PACKET_CODEC
    )

}