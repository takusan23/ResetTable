package io.github.takusan23.resettable.block

import net.minecraft.block.AbstractBlock
import net.minecraft.block.MapColor
import net.minecraft.block.enums.Instrument
import net.minecraft.sound.BlockSoundGroup

/**
 * このMODで追加するブロック一覧
 */
object ResetTableBlocks {

    /** リセットテーブルブロック */
    val RESET_TABLE_BLOCK = ResetTableBlock(AbstractBlock.Settings.create().mapColor(MapColor.BLACK).instrument(Instrument.BASS).strength(2.5F).sounds(BlockSoundGroup.WOOD).burnable())

}