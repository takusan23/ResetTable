package io.github.takusan23.resettable.screen

import com.mojang.blaze3d.systems.RenderSystem
import io.github.takusan23.resettable.tool.ResetTableTool
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier


/**
 * 実際に描画するGUIのためのクラス
 *
 * [handler]はgetterは動くけど、setter系はまじで動かない。
 * */
class ResetTableScreen(
    private val resetTableScreenHandler: ResetTableScreenHandler?,
    private val inventory: PlayerInventory?,
    title: Text?,
) : HandledScreen<ResetTableScreenHandler>(resetTableScreenHandler, inventory, title) {
    private val TEXTURE = Identifier("resettable", "textures/gui/reset_table_gui.png")

    /** 還元スロットのX座標 */
    private val resetSlotPosX = 124

    /** スロットの幅 */
    private val slotWidth = 18

    /** 青色 */
    private val COLOR_BLUE = 0x0000FF

    override fun drawBackground(matrices: MatrixStack?, delta: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.setShader { GameRenderer.getPositionTexShader() }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        RenderSystem.setShaderTexture(0, TEXTURE)
        val x = (width - backgroundWidth) / 2
        val y = (height - backgroundHeight) / 2
        drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight)
    }

    /** テキスト描画はここで */
    override fun drawForeground(matrices: MatrixStack?, mouseX: Int, mouseY: Int) {
        super.drawForeground(matrices, mouseX, mouseY)
        // アイテムが戻せない場合はなんで戻せないのか理由を
        val verify = resetTableScreenHandler?.verifyResultItem()
        if (verify != null) {
            // 文字と色を解決
            val textColorPair = if (verify != ResetTableTool.VerifyResult.SUCCESS) {
                // 失敗時はエラー文言を解決する
                ResetTableTool.resolveUserDescription(verify)
            } else {
                // 成功時はレシピ切り替え番号を
                "${resetTableScreenHandler?.propertyDelegate?.get(1)}/${resetTableScreenHandler?.getRecipePatternCount() ?: -1}" to COLOR_BLUE
            } ?: return
            textRenderer.draw(
                matrices,
                textColorPair.first,
                (resetSlotPosX + (slotWidth / 2f)) - (textRenderer.getWidth(textColorPair.first) / 2), // 真ん中にするため
                60f,
                textColorPair.second
            )
        }
    }

    override fun render(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(matrices)
        super.render(matrices, mouseX, mouseY, delta)
        drawMouseoverTooltip(matrices, mouseX, mouseY)
    }

    override fun init() {
        super.init()
        // 真ん中にGUIタイトルを表示させるため
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2

        // 前のレシピボタン
        addDrawableChild(ButtonWidget(x + resetSlotPosX - 10, y + 58, 10, 10, Text.of("<")) {
            /** ボタンを押したことを [ResetTableScreenHandler] へ通知する */
            client?.interactionManager?.clickButton(handler.syncId, RECIPE_MORE_PREV_BUTTON_ID)
        })
        // 次のレシピボタン
        addDrawableChild(ButtonWidget(x + resetSlotPosX + slotWidth, y + 58, 10, 10, Text.of(">")) {
            client?.interactionManager?.clickButton(handler.syncId, RECIPE_MORE_NEXT_BUTTON_ID)
        })
    }

    companion object {

        /** < -1 ボタンを押したときのid */
        const val RECIPE_MORE_NEXT_BUTTON_ID = 1

        /** > +1 ボタンを押したときのid */
        const val RECIPE_MORE_PREV_BUTTON_ID = 2

    }

}