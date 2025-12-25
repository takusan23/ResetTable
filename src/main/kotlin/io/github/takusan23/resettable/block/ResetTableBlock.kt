package io.github.takusan23.resettable.block

import com.mojang.serialization.MapCodec
import io.github.takusan23.resettable.entity.ResetTableEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Containers
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult


/**
 * リセットテーブルの中身
 *
 * @param settings ブロックの設定（硬さとか。適正ツールはJSONを書かないといけないという初見殺し）
 */
class ResetTableBlock(settings: Properties) : BaseEntityBlock(settings) {

    /** Entityを返す。アイテムを保持するやつ*/
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return ResetTableEntity(pos, state)
    }

    /** デフォルトだと駄目なのでオーバーライドして返り値を変更する */
    override fun getRenderShape(blockState: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    /** ブロックをクリックしたとき */
    override fun useWithoutItem(blockState: BlockState, level: Level, blockPos: BlockPos, player: Player, blockHitResult: BlockHitResult): InteractionResult {
        if (!level.isClientSide) {
            // BlockEntityが手に入るとか
            val screenHandlerFactory = blockState.getMenuProvider(level, blockPos)
            // クライアント側へGUIを開くようお願いする
            player.openMenu(screenHandlerFactory)
        }
        return InteractionResult.SUCCESS
    }

    override fun affectNeighborsAfterRemoval(blockState: BlockState, serverLevel: ServerLevel, blockPos: BlockPos, bl: Boolean) {
        Containers.updateNeighboursAfterDestroy(blockState, serverLevel, blockPos)
    }

    override fun codec(): MapCodec<out BaseEntityBlock> {
        return CODEC
    }

    override fun hasAnalogOutputSignal(blockState: BlockState): Boolean {
        return true
    }

    override fun getAnalogOutputSignal(blockState: BlockState, level: Level, blockPos: BlockPos, direction: Direction): Int {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(blockPos))
    }

    companion object {
        // Fabric 曰くまだ内部では使われていない、がとりあえず返す必要があるとのこと
        // https://fabricmc.net/2023/11/30/1203.html
        private val CODEC = simpleCodec { settings -> ResetTableBlock(settings) }
    }

}