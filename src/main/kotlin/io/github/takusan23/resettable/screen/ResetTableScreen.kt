package io.github.takusan23.resettable.screen

import com.mojang.blaze3d.systems.RenderSystem
import io.github.takusan23.resettable.network.ResetTableNetworks
import io.github.takusan23.resettable.tool.ResetTableTool
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier


/**
 * 実際に描画するGUIのためのクラス
 * */
class ResetTableScreen(private val resetTableScreenHandler: ResetTableScreenHandler?, private val inventory: PlayerInventory?, title: Text?) : HandledScreen<ResetTableScreenHandler>(resetTableScreenHandler, inventory, title) {
    private val TEXTURE = Identifier("resettable", "textures/gui/reset_table_gui.png")

    /** 還元スロットのX座標 */
    private val resetSlotPosX = 124

    /** スロットの幅 */
    private val slotWidth = 18

    /** 青色 */
    private val COLOR_BLUE = 0x0000FF

    private var pageIndex = 0

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
                ResetTableTool.resolveUserDescription(verify)
            } else {
                "${pageIndex}" to COLOR_BLUE
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
        addDrawableChild(ButtonWidget(x + resetSlotPosX - 10, 100, 10, 10, Text.of("<")) {
            sendServer(pageIndex--)
        })
        // 次のレシピボタン
        addDrawableChild(ButtonWidget(x + resetSlotPosX + slotWidth, 100, 10, 10, Text.of(">")) {
            sendServer(pageIndex++)
        })
    }

    /**
     * サーバー側へレシピ切り替え番号変更を通知する
     * */
    private fun sendServer(size: Int) {
        // データを詰めた順番は受け取る側でも同じ
        val buf = PacketByteBufs.create().also {
            it.writeInt(size)
        }
        ClientPlayNetworking.send(ResetTableNetworks.SEND_RECIPE_INDEX_CHANGE, buf)
    }

}