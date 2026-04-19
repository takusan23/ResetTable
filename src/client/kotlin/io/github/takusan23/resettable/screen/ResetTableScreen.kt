package io.github.takusan23.resettable.screen

import io.github.takusan23.resettable.tool.ResetTableTool
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Inventory

/**
 * 実際に描画するGUIのためのクラス。
 * 多分クライアント側しか呼ばれない。
 *
 * [net.minecraft.client.gui.screen.ingame.HandledScreen.handler]はgetterは動くけど、setter系はまじで動かない。
 */
class ResetTableScreen(
    private val resetTableScreenHandler: ResetTableScreenHandler,
    inventory: Inventory,
    title: Component
) : AbstractContainerScreen<ResetTableScreenHandler>(resetTableScreenHandler, inventory, title) {

    override fun init() {
        super.init()
        // 真ん中にGUIタイトルを表示させるため
        titleLabelX = (imageWidth - font.width(title)) / 2
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        super.extractBackground(graphics, mouseX, mouseY, a)
        val x = (width - imageWidth) / 2
        val y = (height - imageHeight) / 2
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0f, 0f, imageWidth, imageHeight, 256, 256)
    }

    /** テキスト描画はここで */
    override fun extractLabels(graphics: GuiGraphicsExtractor, xm: Int, ym: Int) {
        super.extractLabels(graphics, xm, ym)
        // アイテムが戻せない場合はなんで戻せないのか理由を
        val verify = resetTableScreenHandler.recipeVerifyResult
        // エラー時は利用できない理由を
        val textColorPair = ResetTableTool.resolveUserDescription(verify) ?: return
        // テキスト描画
        graphics.text(
            font,
            textColorPair.first,
            ((RESET_SLOT_POS_X + (SLOT_WIDTH / 2f)) - (font.width(textColorPair.first) / 2)).toInt(), // 真ん中にするため
            60,
            textColorPair.second,
            false
        )
    }

    companion object {

        private val TEXTURE = Identifier.fromNamespaceAndPath("resettable", "textures/gui/reset_table_gui.png")

        /** 還元スロットのX座標 */
        private const val RESET_SLOT_POS_X = 124

        /** スロットの幅 */
        private const val SLOT_WIDTH = 18

    }

}