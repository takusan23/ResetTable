package io.github.takusan23.resettable.network

import io.github.takusan23.resettable.tool.ResetTableTool
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.core.BlockPos

/**
 * リセットテーブルできない、元のレシピに戻せないエラーをネットワークでやり取りするデータクラス。
 * このクラスをシリアライズ・デシリアライズしてクライアント・サーバー間をやり取りする。
 *
 * @see [https://fabricmc.net/wiki/tutorial:networking]
 * @param blockPos 多分使ってない
 * @param verifyResult 元のレシピへ戻せない理由。[ResetTableTool.VerifyResult]
 */
data class ResetTableErrorPayload(
    val blockPos: BlockPos,
    val verifyResult: ResetTableTool.VerifyResult
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = ID

    companion object {

        val ID = CustomPacketPayload.Type<ResetTableErrorPayload>(ResetTableNetworkIds.RESET_TABLE_ERROR_NETWORK_ID)

        val CODEC = StreamCodec.ofMember<RegistryFriendlyByteBuf, ResetTableErrorPayload>(
            /* encoder = */ { data, buf ->
                buf.writeBlockPos(data.blockPos)
                buf.writeInt(data.verifyResult.ordinal)
            },
            /* decoder = */ { buf ->
                ResetTableErrorPayload(
                    blockPos = buf.readBlockPos(),
                    verifyResult = ResetTableTool.VerifyResult.entries[buf.readInt()]
                )
            }
        )
    }

}