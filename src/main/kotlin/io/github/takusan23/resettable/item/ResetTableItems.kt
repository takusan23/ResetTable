package io.github.takusan23.resettable.item

import io.github.takusan23.resettable.block.ResetTableBlocks
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys

/**
 * このMODで追加するアイテム一覧
 */
object ResetTableItems {

    // レジストリキー
    private val KEY_RESET_TABLE_BLOCK_ITEM = RegistryKey.of(RegistryKeys.ITEM, ResetTableBlocks.ID_RESET_TABLE_BLOCK)

    /** リセットテーブルのブロックを壊したときのアイテム */
    val RESET_TABLE_BLOCK_ITEM = BlockItem(ResetTableBlocks.RESET_TABLE_BLOCK, Item.Settings().useBlockPrefixedTranslationKey().registryKey(KEY_RESET_TABLE_BLOCK_ITEM))

    /** アイテムを登録する */
    fun registry() {
        Registry.register(Registries.ITEM, KEY_RESET_TABLE_BLOCK_ITEM, RESET_TABLE_BLOCK_ITEM)
    }
}