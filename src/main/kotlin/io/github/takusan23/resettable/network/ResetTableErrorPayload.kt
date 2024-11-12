package io.github.takusan23.resettable.network

import io.github.takusan23.resettable.tool.ResetTableTool
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.math.BlockPos

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
) : CustomPayload {

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID

    companion object {

        val ID = CustomPayload.Id<ResetTableErrorPayload>(ResetTableNetworkIds.RESET_TABLE_ERROR_NETWORK_ID)

        val CODEC = PacketCodec.of<RegistryByteBuf, ResetTableErrorPayload>(
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