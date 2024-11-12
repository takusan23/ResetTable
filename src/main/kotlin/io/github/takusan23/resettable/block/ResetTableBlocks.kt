package io.github.takusan23.resettable.block

import net.minecraft.block.AbstractBlock
import net.minecraft.block.MapColor
import net.minecraft.block.enums.NoteBlockInstrument
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.util.Identifier

/**
 * このMODで追加するブロック一覧
 */
object ResetTableBlocks {

    // ブロック ID。アイテムブロックで使うので public
    val ID_RESET_TABLE_BLOCK = Identifier.of("resettable", "reset_table_block")

    // レジストリキー
    private val KEY_RESET_TABLE_BLOCK = RegistryKey.of(RegistryKeys.BLOCK, ID_RESET_TABLE_BLOCK)

    /** リセットテーブルブロック */
    val RESET_TABLE_BLOCK = ResetTableBlock(AbstractBlock.Settings.create().mapColor(MapColor.BLACK).instrument(NoteBlockInstrument.BASS).strength(2.5F).sounds(BlockSoundGroup.WOOD).burnable().registryKey(KEY_RESET_TABLE_BLOCK))

    /** ブロックを追加する */
    fun registry() {
        Registry.register(Registries.BLOCK, KEY_RESET_TABLE_BLOCK, RESET_TABLE_BLOCK)
    }

}