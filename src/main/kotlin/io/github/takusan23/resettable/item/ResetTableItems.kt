package io.github.takusan23.resettable.item

import io.github.takusan23.resettable.block.ResetTableBlocks
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.Registries

/**
 * このMODで追加するアイテム一覧
 */
object ResetTableItems {

    // レジストリキー
    private val KEY_RESET_TABLE_BLOCK_ITEM = ResourceKey.create(Registries.ITEM, ResetTableBlocks.ID_RESET_TABLE_BLOCK)

    /** リセットテーブルのブロックを壊したときのアイテム */
    val RESET_TABLE_BLOCK_ITEM = BlockItem(ResetTableBlocks.RESET_TABLE_BLOCK, Item.Properties().useBlockDescriptionPrefix().setId(KEY_RESET_TABLE_BLOCK_ITEM))

    /** アイテムを登録する */
    fun registry() {
        Registry.register(BuiltInRegistries.ITEM, KEY_RESET_TABLE_BLOCK_ITEM, RESET_TABLE_BLOCK_ITEM)
    }
}