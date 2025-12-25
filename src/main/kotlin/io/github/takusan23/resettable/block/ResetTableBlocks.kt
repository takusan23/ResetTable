package io.github.takusan23.resettable.block

import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.SoundType
import net.minecraft.resources.Identifier

/**
 * このMODで追加するブロック一覧
 */
object ResetTableBlocks {

    // ブロック ID。アイテムブロックで使うので public
    val ID_RESET_TABLE_BLOCK = Identifier.fromNamespaceAndPath("resettable", "reset_table_block")

    // レジストリキー
    private val KEY_RESET_TABLE_BLOCK = ResourceKey.create(Registries.BLOCK, ID_RESET_TABLE_BLOCK)

    /** リセットテーブルブロック */
    val RESET_TABLE_BLOCK = ResetTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava().setId(KEY_RESET_TABLE_BLOCK))

    /** ブロックを追加する */
    fun registry() {
        Registry.register(BuiltInRegistries.BLOCK, KEY_RESET_TABLE_BLOCK, RESET_TABLE_BLOCK)
    }

}