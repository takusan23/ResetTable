package io.github.takusan23.resettable.entity

import io.github.takusan23.resettable.block.ResetTableBlocks
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

/**
 * このMODで利用するEntity
 * */
object ResetTableEntities {

    /**
     * リセットテーブルブロックのEntity
     * */
    val RESET_TABLE_BLOCK_ENTITY: BlockEntityType<ResetTableEntity> = FabricBlockEntityTypeBuilder.create(
        { pos, state -> ResetTableEntity(pos, state) },
        ResetTableBlocks.RESET_TABLE_BLOCK
    ).build(null)

    /** Entity を追加する */
    fun registry() {
        Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of("resettable", "reset_table_block"), RESET_TABLE_BLOCK_ENTITY)
    }

}