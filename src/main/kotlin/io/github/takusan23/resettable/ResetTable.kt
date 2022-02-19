package io.github.takusan23.resettable

import io.github.takusan23.resettable.block.ResetTableBlocks
import io.github.takusan23.resettable.entity.ResetTableEntities
import io.github.takusan23.resettable.item.ResetTableItems
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

/**
 * エントリーポイント
 *
 * 多分最初にここの関数がFabricによって呼ばれる。
 * */
@Suppress("unused")
fun init() {
    // アイテムの追加
    registerItems()
    // ブロックの追加
    registerBlocks()
    // Entityの追加
    registerEntities()
}

/**
 * エンティティをFabricに登録する。
 *
 * MOD初期化時に呼ぶ
 * */
private fun registerEntities() {
    Registry.register(Registry.BLOCK_ENTITY_TYPE, Identifier("resettable", "reset_table_block"), ResetTableEntities.RESET_TABLE_BLOCK_ENTITY)
}

/**
 * アイテムをFabricに登録する。
 *
 * MOD初期化時に呼ぶ
 * */
private fun registerItems() {
    Registry.register(Registry.ITEM, Identifier("resettable", "reset_table_block"), ResetTableItems.RESET_TABLE_BLOCK_ITEM)
}

/**
 * ブロックをFabricに登録する。
 *
 * MOD初期化時に呼ぶ
 * */
private fun registerBlocks() {
    Registry.register(Registry.BLOCK, Identifier("resettable", "reset_table_block"), ResetTableBlocks.RESET_TABLE_BLOCK)
}