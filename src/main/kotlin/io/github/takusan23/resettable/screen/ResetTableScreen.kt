package io.github.takusan23.resettable.screen

import com.mojang.blaze3d.systems.RenderSystem
import io.github.takusan23.resettable.tool.ResetTableTool
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

/**
 * 実際に描画するGUIのためのクラス
 * */
class ResetTableScreen(private val resetTableScreenHandler: ResetTableScreenHandler?, inventory: PlayerInventory?, title: Text?) : HandledScreen<ResetTableScreenHandler>(resetTableScreenHandler, inventory, title) {
    // バニラの作業台の背景画像をパク..借りる
    private val TEXTURE = Identifier("resettable", "textures/gui/reset_table_gui.png")

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
        // アイテムが戻せない場合はなんで戻せない理由を
        resetTableScreenHandler?.verifyResultItem()?.also { status ->
            // 文字と色を解決
            val textColorPair = ResetTableTool.resolveUserDescription(status) ?: return
            textRenderer.draw(
                matrices,
                textColorPair.first,
                133f - (textRenderer.getWidth(textColorPair.first) / 2), // 真ん中にするため
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
    }

}