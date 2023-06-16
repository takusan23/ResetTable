package io.github.takusan23.resettable.block

import io.github.takusan23.resettable.entity.ResetTableEntity
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.screen.ScreenHandler
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.ItemScatterer
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World


/**
 * リセットテーブルの中身
 *
 * @param settings ブロックの設定（硬さとか。適正ツールはJSONを書かないといけないという初見殺し）
 */
class ResetTableBlock(settings: Settings?) : BlockWithEntity(settings) {

    /** Entityを返す。アイテムを保持するやつ*/
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return ResetTableEntity(pos, state)
    }

    /** デフォルトだと駄目なのでオーバーライドして返り値を変更する */
    override fun getRenderType(state: BlockState?): BlockRenderType {
        return BlockRenderType.MODEL
    }

    /** ブロックをクリックしたとき */
    override fun onUse(state: BlockState?, world: World?, pos: BlockPos?, player: PlayerEntity?, hand: Hand?, hit: BlockHitResult?): ActionResult {
        if (world?.isClient == false && state != null) {
            // BlockEntityが手に入るとか
            val screenHandlerFactory = state.createScreenHandlerFactory(world, pos)
            // クライアント側へGUIを開くようお願いする
            player?.openHandledScreen(screenHandlerFactory)
        }
        return ActionResult.SUCCESS
    }

    /** ブロックを壊したとき。チェスト破壊じにドロップさせる処理なんかができる */
    override fun onStateReplaced(state: BlockState?, world: World?, pos: BlockPos?, newState: BlockState?, moved: Boolean) {
        if (state?.block !== newState?.block) {
            val blockEntity = world?.getBlockEntity(pos)
            if (blockEntity is ResetTableEntity) {
                ItemScatterer.spawn(world, pos, blockEntity)
                world.updateComparators(pos, this)
            }
            super.onStateReplaced(state, world, pos, newState, moved)
        }
    }

    override fun hasComparatorOutput(state: BlockState?): Boolean {
        return true
    }

    override fun getComparatorOutput(state: BlockState?, world: World, pos: BlockPos?): Int {
        return ScreenHandler.calculateComparatorOutput(world.getBlockEntity(pos))
    }

}