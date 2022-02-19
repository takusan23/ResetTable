package io.github.takusan23.resettable.block

import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.Block
import net.minecraft.block.Material
import net.minecraft.sound.BlockSoundGroup

/**
 * このMODで追加するブロック一覧
 * */
object ResetTableBlocks {

    /** リセットテーブル */
    val RESET_TABLE_BLOCK = Block(FabricBlockSettings.of(Material.WOOD).strength(2.5F).sounds(BlockSoundGroup.WOOD))

}