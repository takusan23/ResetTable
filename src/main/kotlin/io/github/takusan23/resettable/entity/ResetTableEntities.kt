package io.github.takusan23.resettable.entity

import io.github.takusan23.resettable.block.ResetTableBlocks
import net.minecraft.block.entity.BlockEntityType

/**
 * このMODで利用するEntity
 * */
object ResetTableEntities {

    /**
     * リセットテーブルブロックのEntity
     * */
    val RESET_TABLE_BLOCK_ENTITY: BlockEntityType<ResetTableEntity> = BlockEntityType.Builder.create(
        { pos, state -> ResetTableEntity(pos, state) },
        ResetTableBlocks.RESET_TABLE_BLOCK
    ).build(null)

}