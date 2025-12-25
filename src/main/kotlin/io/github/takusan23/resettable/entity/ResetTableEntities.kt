package io.github.takusan23.resettable.entity

import io.github.takusan23.resettable.block.ResetTableBlocks
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.resources.Identifier

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
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath("resettable", "reset_table_block"), RESET_TABLE_BLOCK_ENTITY)
    }

}