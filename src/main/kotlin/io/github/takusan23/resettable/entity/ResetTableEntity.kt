package io.github.takusan23.resettable.entity

import io.github.takusan23.resettable.screen.ResetTableScreenHandler
import io.github.takusan23.resettable.screen.ResetTableScreenHandlerServerClientData
import io.github.takusan23.resettable.tool.ResetTableTool
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.ContainerHelper
import net.minecraft.world.MenuProvider
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput


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
    state: BlockState
) : BlockEntity(ResetTableEntities.RESET_TABLE_BLOCK_ENTITY, pos, state), MenuProvider, ExtendedMenuProvider<ResetTableScreenHandlerServerClientData>, ImplementedInventory, WorldlyContainer {

    /**
     * リセットテーブルのインベントリ
     *
     * 3x3 の分と還元スロットのために +1
     * */
    private val inventory = NonNullList.withSize(10, ItemStack.EMPTY)

    /** アイテム変更コールバックの配列 */
    private val itemChangeCallbackList = mutableListOf<() -> Unit>()

    /**
     * Entityが持っているアイテムを返す
     *
     * @return 保持しているアイテム
     * */
    override fun getItems(): NonNullList<ItemStack> {
        return inventory
    }

    /** GUIを返す？ */
    override fun createMenu(i: Int, inventory: Inventory, player: Player): AbstractContainerMenu? {
        return ResetTableScreenHandler(i, inventory, this)
    }

    /** ホッパー等からアクセスできるスロットを返す */
    override fun getSlotsForFace(direction: Direction): IntArray {
        return when (direction) {
            Direction.UP -> intArrayOf(RESET_TABLE_RESET_ITEM_SLOT)
            Direction.DOWN -> (0..8).toList().toIntArray()
            else -> intArrayOf()
        }
    }

    /** アイテムをホッパー等から受け付けるか */
    override fun canPlaceItemThroughFace(i: Int, itemStack: ItemStack, direction: Direction?): Boolean {
        // ItemStackが引数で貰えますが、常に count=1 で足りているかわからないため、
        // いま還元スロットに入っているアイテムと同じ場合と空っぽの場合に受け付けます
        val resetSlotItemStack = getItem(RESET_TABLE_RESET_ITEM_SLOT)
        return resetSlotItemStack.isEmpty || resetSlotItemStack.`is`(itemStack.item)
    }

    /** アイテムを取り出せるか */
    override fun canTakeItemThroughFace(i: Int, itemStack: ItemStack, direction: Direction): Boolean {
        // 3x3 の範囲内ならok
        return i in 0..8
    }

    /** インベントリを保存する */
    override fun saveAdditional(valueOutput: ValueOutput) {
        super.saveAdditional(valueOutput)
        ContainerHelper.saveAllItems(valueOutput, this.inventory)
    }

    /** 保存したインベントリを取り出す */
    override fun loadAdditional(valueInput: ValueInput) {
        super.loadAdditional(valueInput)
        ContainerHelper.loadAllItems(valueInput, this.inventory)
    }

    override fun getDisplayName(): Component {
        // ブロックのローカライズテキストをそのまま利用する
        return Component.translatable(blockState.block.descriptionId)
    }

    /**
     * サーバー側で呼ばれる。
     *
     * クライアントに贈りたいデータをここで詰めておく。
     * */
    override fun getScreenOpeningData(p0: ServerPlayer): ResetTableScreenHandlerServerClientData {
        // クライアント側（GUI）でブロックの位置を知りたいので渡しておく
        return ResetTableScreenHandlerServerClientData(worldPosition)
    }

    /**
     * 多分アイテムを入れたりしたときに呼ばれる
     *
     * ここでレシピ検索をしている
     * */
    override fun setChanged() {
        updateResultItems()
        itemChangeCallbackList.forEach { it.invoke() }
    }

    /**
     * いまの還元スロットに入っているアイテムのレシピを探して、材料スロット（3x3）に入れる
     *
     * 既に材料スロットに入っている場合は戻さない、けど前回と同じレシピだった場合は戻す
     * */
    private fun updateResultItems() {
        val serverWorld = (level as? ServerLevel) ?: return
        val currentResetSlotItemStack = getItem(RESET_TABLE_RESET_ITEM_SLOT)
        val currentRecipeResolveDataList = ResetTableTool.findCraftingMaterial(serverWorld, currentResetSlotItemStack)
        if (isMaterialSlotEmpty()) {
            currentRecipeResolveDataList
                ?.getOrNull(0)
                ?.also { recipeResolveData ->
                    recipeResolveData
                        .recipePatternFormattedList
                        .forEachIndexed { index, itemStack -> setItem(index, itemStack) }
                    recipeResolveData
                        .resolveSlotItemStack
                        .also { setItem(RESET_TABLE_RESET_ITEM_SLOT, it) }
                }
        } else {
            // スロット空いてないけど、今のスロットと同じ中身だった場合
            // ただし材料スロットが1スタックを超えるようなら何もしない
            currentRecipeResolveDataList
                ?.firstOrNull { recipeResolveData ->
                    isEqualItemByItemStackList(getMaterialSlotItemStackList(), recipeResolveData.recipePatternFormattedList)
                            && isInsertableMaterialSlot(recipeResolveData.recipePatternFormattedList[0].count)
                }?.also { recipeResolveData ->
                    // アイテム数を増やす
                    getMaterialSlotItemStackList()
                        .map { it.copy().apply { count += recipeResolveData.recipePatternFormattedList[0].count } }
                        .forEachIndexed { index, itemStack -> setItem(index, itemStack) }
                    // 割り切れなかったアイテムを還元スロットへ
                    setItem(RESET_TABLE_RESET_ITEM_SLOT, recipeResolveData.resolveSlotItemStack)
                }
        }
    }

    /**
     * アイテム変更コールバックを登録する。使ってないけどいつか使うかも
     *
     * @param callback アイテム変更時に呼ばれる関数
     * */
    @Suppress("unused")
    fun addItemChangeCallback(callback: () -> Unit) {
        itemChangeCallbackList.add(callback)
    }

    /**
     * 材料スロットが空っぽかどうか
     *
     * @return 3x3 のスロットが空っぽならtrue
     * */
    private fun isMaterialSlotEmpty(): Boolean {
        return getMaterialSlotItemStackList().all { it.isEmpty }
    }

    /**
     * 材料スロットに指定したアイテム数を入れると、1スタックを超えてしまう場合はfalseを返す
     *
     * @param addCount 追加数
     * @return どれか一つでも1スタックを超える場合はfalse
     * */
    private fun isInsertableMaterialSlot(addCount: Int): Boolean {
        return getMaterialSlotItemStackList().all { it.count + addCount <= ITEM_STACK_MAX_VALUE }
    }

    /**
     * 材料スロットのアイテムを取得する
     *
     * @return 3x3 のアイテムスロット
     * */
    private fun getMaterialSlotItemStackList(): List<ItemStack> {
        return (0..8).map { getItem(it) }
    }

    /**
     * 2つのItemStack配列を見て、同じアイテムが入っている場合はtrue。スタック数等は見ていない
     *
     * @param list1 ItemStackの配列
     * @param list2 ItemStackの配列
     * @return 同じ場合はtrue
     * */
    private fun isEqualItemByItemStackList(list1: List<ItemStack>, list2: List<ItemStack>): Boolean {
        return (0..kotlin.math.max(list1.size, list2.size)).all { index ->
            val list1Item = list1.getOrNull(index) ?: ItemStack.EMPTY
            val list2Item = list2.getOrNull(index) ?: ItemStack.EMPTY
            list1Item.item == list2Item.item
        }
    }

    companion object {

        /** リセットしたいアイテムが入るスロット番号 */
        const val RESET_TABLE_RESET_ITEM_SLOT = 9

        /** 1スタック */
        const val ITEM_STACK_MAX_VALUE = 64
    }
}