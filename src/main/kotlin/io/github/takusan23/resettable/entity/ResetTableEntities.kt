package io.github.takusan23.resettable.entity

import io.github.takusan23.resettable.block.ResetTableBlocks
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder

/**
 * このMODで利用するEntity
 * */
object ResetTableEntities {

    /**
     * リセットテーブルブロックのEntity
     * */
    val RESET_TABLE_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(
        { pos, state -> ResetTableEntity(pos, state) },
        ResetTableBlocks.RESET_TABLE_BLOCK
    ).build(null)

}