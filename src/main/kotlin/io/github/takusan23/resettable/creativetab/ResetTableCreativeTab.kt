package io.github.takusan23.resettable.creativetab

import io.github.takusan23.resettable.item.ResetTableItems
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier

/**
 * クリエイティブタブ
 * */
object ResetTableCreativeTab {

    /**
     * クリエイティブタブ
     *
     * 型指定を消してはいけない
     * */
    val CREATIVE_TAB: ItemGroup = FabricItemGroupBuilder
        .create(Identifier("resettable", "resettable_creative_tab"))
        .icon { ItemStack(ResetTableItems.RESET_TABLE_BLOCK_ITEM) }
        .build()
}