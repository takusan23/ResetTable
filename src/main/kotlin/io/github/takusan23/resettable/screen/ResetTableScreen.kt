package io.github.takusan23.resettable.screen

import io.github.takusan23.resettable.tool.ResetTableTool
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier


/**
 * 実際に描画するGUIのためのクラス
 *
 * [handler]はgetterは動くけど、setter系はまじで動かない。
 */
class ResetTableScreen(
    private val resetTableScreenHandler: ResetTableScreenHandler?,
    inventory: PlayerInventory?,
    title: Text?,
) : HandledScreen<ResetTableScreenHandler>(resetTableScreenHandler, inventory, title) {

    override fun init() {
        super.init()
        // 真ん中にGUIタイトルを表示させるため
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2
    }

    override fun drawBackground(context: DrawContext?, delta: Float, mouseX: Int, mouseY: Int) {
        val x = (width - backgroundWidth) / 2
        val y = (height - backgroundHeight) / 2
        context?.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight)
    }

    /** テキスト描画はここで */
    override fun drawForeground(context: DrawContext?, mouseX: Int, mouseY: Int) {
        super.drawForeground(context, mouseX, mouseY)
        // アイテムが戻せない場合はなんで戻せないのか理由を
        val verify = resetTableScreenHandler?.verifyResultItem()
        if (verify != null) {
            // エラー時は利用できない理由を
            val textColorPair = ResetTableTool.resolveUserDescription(verify) ?: return
            // テキスト描画
            context?.drawText(
                textRenderer,
                textColorPair.first,
                ((RESET_SLOT_POS_X + (SLOT_WIDTH / 2f)) - (textRenderer.getWidth(textColorPair.first) / 2)).toInt(), // 真ん中にするため
                60,
                textColorPair.second,
                false
            )
        }
    }

    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        this.drawMouseoverTooltip(context, mouseX, mouseY)
    }

    companion object {

        private val TEXTURE = Identifier("resettable", "textures/gui/reset_table_gui.png")

        /** 還元スロットのX座標 */
        private const val RESET_SLOT_POS_X = 124

        /** スロットの幅 */
        private const val SLOT_WIDTH = 18

    }

}