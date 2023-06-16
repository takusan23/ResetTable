package io.github.takusan23.resettable

import io.github.takusan23.resettable.block.ResetTableBlocks
import io.github.takusan23.resettable.creativetab.ResetTableCreativeTab
import io.github.takusan23.resettable.entity.ResetTableEntities
import io.github.takusan23.resettable.item.ResetTableItems
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

/**
 * エントリーポイント
 *
 * 多分最初にここの関数がFabricによって呼ばれる。
 */
@Suppress("unused")
fun init() {
    // アイテムの追加
    registerItems()
    // ブロックの追加
    registerBlocks()
    // Entityの追加
    registerEntities()
    // クリエイティブタブの追加
    registerCreativeTab()
}

/**
 * クリエイティブタブをFabricに登録する。
 *
 * MOD初期化時に呼ぶ
 */
fun registerCreativeTab() {
    Registry.register(Registries.ITEM_GROUP, Identifier("resettable", "resettable_creative_tab"), ResetTableCreativeTab.CREATIVE_TAB)
}

/**
 * エンティティをFabricに登録する。
 *
 * MOD初期化時に呼ぶ
 */
private fun registerEntities() {
    Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier("resettable", "reset_table_block"), ResetTableEntities.RESET_TABLE_BLOCK_ENTITY)
}

/**
 * アイテムをFabricに登録する。
 *
 * MOD初期化時に呼ぶ
 */
private fun registerItems() {
    Registry.register(Registries.ITEM, Identifier("resettable", "reset_table_block"), ResetTableItems.RESET_TABLE_BLOCK_ITEM)
}

/**
 * ブロックをFabricに登録する。
 *
 * MOD初期化時に呼ぶ
 */
private fun registerBlocks() {
    Registry.register(Registries.BLOCK, Identifier("resettable", "reset_table_block"), ResetTableBlocks.RESET_TABLE_BLOCK)
}