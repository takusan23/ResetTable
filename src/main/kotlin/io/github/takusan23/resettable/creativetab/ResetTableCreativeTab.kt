package io.github.takusan23.resettable.creativetab

import io.github.takusan23.resettable.item.ResetTableItems
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.text.Text

/**
 * クリエイティブタブ
 * */
object ResetTableCreativeTab {

    /**
     * クリエイティブタブ
     *
     * 型指定を消してはいけない
     */
    val CREATIVE_TAB = FabricItemGroup.builder()
        .displayName(Text.translatable("itemGroup.resettable.resettable_creative_tab")) // displayName は明示的に呼び出す必要がある
        .icon { ItemStack(ResetTableItems.RESET_TABLE_BLOCK_ITEM) }
        .entries { _, entries ->
            entries.add(ResetTableItems.RESET_TABLE_BLOCK_ITEM)
        }
        .build()
}