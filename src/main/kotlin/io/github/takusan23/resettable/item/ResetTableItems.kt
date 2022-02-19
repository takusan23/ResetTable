package io.github.takusan23.resettable.item

import io.github.takusan23.resettable.block.ResetTableBlocks
import io.github.takusan23.resettable.creativetab.ResetTableCreativeTab
import net.minecraft.item.BlockItem
import net.minecraft.item.Item

/**
 * このMODで追加するアイテム一覧
 * */
object ResetTableItems {

    /** リセットテーブルのブロックを壊したときのアイテム */
    val RESET_TABLE_BLOCK_ITEM = BlockItem(ResetTableBlocks.RESET_TABLE_BLOCK, Item.Settings().group(ResetTableCreativeTab.CREATIVE_TAB))

}