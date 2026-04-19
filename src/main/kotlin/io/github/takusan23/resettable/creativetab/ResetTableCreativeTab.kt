package io.github.takusan23.resettable.creativetab

import io.github.takusan23.resettable.item.ResetTableItems
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

/**
 * クリエイティブタブ
 * */
object ResetTableCreativeTab {

    /**
     * クリエイティブタブ
     *
     * 型指定を消してはいけない
     */
    val CREATIVE_TAB = FabricCreativeModeTab.builder()
        .title(Component.translatable("itemGroup.resettable.resettable_creative_tab")) // displayName は明示的に呼び出す必要がある
        .icon { ItemStack(ResetTableItems.RESET_TABLE_BLOCK_ITEM) }
        .displayItems { _, entries ->
            entries.accept(ResetTableItems.RESET_TABLE_BLOCK_ITEM)
        }
        .build()
}