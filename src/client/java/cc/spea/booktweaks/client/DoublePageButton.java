package cc.spea.booktweaks.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * A page button that renders double arrows (<<, >>) for jump-to-start/end functionality.
 */
public class DoublePageButton extends PageButton {
    private static final ResourceLocation PAGE_FORWARD_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/page_forward_highlighted");
    private static final ResourceLocation PAGE_FORWARD_SPRITE = ResourceLocation.withDefaultNamespace("widget/page_forward");
    private static final ResourceLocation PAGE_BACKWARD_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/page_backward_highlighted");
    private static final ResourceLocation PAGE_BACKWARD_SPRITE = ResourceLocation.withDefaultNamespace("widget/page_backward");

    private final boolean isForward;

    public DoublePageButton(int x, int y, boolean isForward, OnPress onPress, boolean playTurnSound) {
        super(x, y, isForward, onPress, playTurnSound);
        this.isForward = isForward;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation sprite;
        if (this.isForward) {
            sprite = this.isHoveredOrFocused() ? PAGE_FORWARD_HIGHLIGHTED_SPRITE : PAGE_FORWARD_SPRITE;
        } else {
            sprite = this.isHoveredOrFocused() ? PAGE_BACKWARD_HIGHLIGHTED_SPRITE : PAGE_BACKWARD_SPRITE;
        }

        // Render the arrow twice to create a double arrow effect
        int offset = this.isForward ? 6 : -6;
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX() + offset, this.getY(), 23, 13);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), 23, 13);
    }
}
