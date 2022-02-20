package io.github.takusan23.resettable.entity

import io.github.takusan23.resettable.screen.ResetTableScreenHandler
import io.github.takusan23.resettable.tool.ResetTableTool
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

/**
 * リセットテーブルが持つEntity
 *
 * 内部でアイテムを保持しておくクラス。還元スロットは普通の作業台で言うところの完成品スロットの部分です。indexだと9
 *
 * [SidedInventory]を継承したのでホッパーで自動化できる・・・？
 *
 * @param pos [ResetTableEntities] 参照
 * @param state [ResetTableEntities] 参照
 * */
class ResetTableEntity(
    pos: BlockPos,
    state: BlockState,
) : BlockEntity(ResetTableEntities.RESET_TABLE_BLOCK_ENTITY, pos, state), NamedScreenHandlerFactory, ImplementedInventory, SidedInventory {

    /**
     * リセットテーブルのインベントリ
     *
     * 3x3 の分と還元スロットのために +1
     * */
    private val inventory = DefaultedList.ofSize(10, ItemStack.EMPTY)

    /**
     * Entityが持っているアイテムを返す
     *
     * @return 保持しているアイテム
     * */
    override fun getItems(): DefaultedList<ItemStack> {
        return inventory
    }

    /** GUIを返す？ */
    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler {
        return ResetTableScreenHandler(syncId, playerInventory, this)
    }

    /** ホッパー等からアクセスできるスロットを返す */
    override fun getAvailableSlots(side: Direction?): IntArray {
        return when (side) {
            Direction.UP -> intArrayOf(RESET_TABLE_RESET_ITEM_SLOT)
            Direction.DOWN -> (0..8).toList().toIntArray()
            else -> intArrayOf()
        }
    }

    /** アイテムをホッパー等から受け付けるか */
    override fun canInsert(slot: Int, stack: ItemStack?, dir: Direction?): Boolean {
        // ItemStackが引数で貰えますが、常に count=1 で足りているかわからないため、
        // いま還元スロットに入っているアイテムと同じ場合と空っぽの場合に受け付けます
        val resetSlotItemStack = getStack(RESET_TABLE_RESET_ITEM_SLOT)
        return resetSlotItemStack.isEmpty || resetSlotItemStack.isOf(stack?.item)
    }

    /** アイテムを取り出せるか */
    override fun canExtract(slot: Int, stack: ItemStack?, dir: Direction?): Boolean {
        // 3x3 の範囲内ならok
        return slot in 0..8
    }

    /** インベントリを保存する */
    override fun writeNbt(nbt: NbtCompound?) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, this.inventory)
    }

    /** 保存したインベントリを取り出す */
    override fun readNbt(nbt: NbtCompound?) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, this.inventory)
    }

    override fun getDisplayName(): Text {
        // ブロックのローカライズテキストをそのまま利用する
        return TranslatableText(cachedState.block.translationKey)
    }

    /**
     * 多分アイテムを入れたりしたときに呼ばれる
     *
     * ここでレシピ検索をしている
     * */
    override fun markDirty() {
        updateResult()
    }

    /** いまの還元スロットに入っているアイテムのレシピを探して、材料スロット（3x3）に入れる */
    private fun updateResult() {
        val nonNullWorld = world ?: return
        val resetSlotItemStack = getStack(RESET_TABLE_RESET_ITEM_SLOT)
        // 作るのに必要な材料を返す
        val recipeResolveData = ResetTableTool.findCraftingMaterial(nonNullWorld, resetSlotItemStack)
        // 一度だけ。もとに戻したアイテムが入るスロットがからじゃない場合も受け付けない
        if (recipeResolveData != null && isMaterialSlotEmpty()) {
            recipeResolveData.resultItemStack.forEachIndexed { index, itemStack -> setStack(index, itemStack) }
            // 戻したので完成品スロットをクリアするか、戻しきれなかったアイテムを入れる
            setStack(RESET_TABLE_RESET_ITEM_SLOT, recipeResolveData.resolveSlotItemStack)
        }
    }

    /**
     * 戻したアイテムが入るスロットが空っぽかどうか
     *
     * @return 3x3 のスロットが空っぽならtrue
     * */
    private fun isMaterialSlotEmpty(): Boolean {
        return (0..8).map { getStack(it) }.all { it.isEmpty }
    }


    companion object {

        /** リセットしたいアイテムが入るスロット番号 */
        const val RESET_TABLE_RESET_ITEM_SLOT = 9

    }
}