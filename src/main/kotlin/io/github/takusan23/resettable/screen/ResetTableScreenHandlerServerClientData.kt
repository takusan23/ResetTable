package io.github.takusan23.resettable.screen

import net.minecraft.network.RegistryByteBuf
import net.minecraft.util.math.BlockPos

/**
 * ブロックの位置を[ResetTableScreenHandler]へ渡すための
 * いままでは[net.minecraft.network.PacketByteBuf]でやり取りしてたけど、なんかうまく動かない。
 * ので、データクラスを作って、シリアライズ・デシリアライズできるようにする。
 *
 * @param blockPos ブロックの位置
 */
data class ResetTableScreenHandlerServerClientData(
    val blockPos: BlockPos
) {

    companion object {

        /** [RegistryByteBuf]へ書き込む */
        fun write(data: ResetTableScreenHandlerServerClientData, buf: RegistryByteBuf) {
            buf.writeBlockPos(data.blockPos)
        }

        /** [RegistryByteBuf]から読み出す */
        fun read(buf: RegistryByteBuf): ResetTableScreenHandlerServerClientData {
            return ResetTableScreenHandlerServerClientData(buf.readBlockPos())
        }

    }
}