package io.github.takusan23.resettable

import io.github.takusan23.resettable.block.ResetTableBlocks
import io.github.takusan23.resettable.creativetab.ResetTableCreativeTab
import io.github.takusan23.resettable.entity.ResetTableEntities
import io.github.takusan23.resettable.item.ResetTableItems
import io.github.takusan23.resettable.network.ResetTableErrorPayload
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
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
    ResetTableItems.registry()
    // ブロックの追加
    ResetTableBlocks.registry()
    // Entityの追加
    ResetTableEntities.registry()
    // クリエイティブタブの追加
    Registry.register(Registries.ITEM_GROUP, Identifier.of("resettable", "resettable_creative_tab"), ResetTableCreativeTab.CREATIVE_TAB)
    // ネットワークの追加（クライアント・サーバー間でやり取りする）
    PayloadTypeRegistry.playS2C().register(ResetTableErrorPayload.ID, ResetTableErrorPayload.CODEC)
}
