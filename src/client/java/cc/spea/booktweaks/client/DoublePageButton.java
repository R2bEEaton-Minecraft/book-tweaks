package cc.spea.booktweaks.client;

#if MC_VER >= MC_26_1
import net.minecraft.client.gui.GuiGraphicsExtractor;
#else
import net.minecraft.client.gui.GuiGraphics;
#endif
import net.minecraft.client.gui.screens.inventory.PageButton;
#if MC_VER >= MC_1_21_6
import net.minecraft.client.renderer.RenderPipelines;
#elif MC_VER >= MC_1_21_2
import net.minecraft.client.renderer.RenderType;
#endif
#if MC_VER >= MC_1_21_11
import net.minecraft.resources.Identifier;
#else
import net.minecraft.resources.ResourceLocation;
#endif

/**
 * A page button that renders double arrows (<<, >>) for jump-to-start/end functionality.
 */
public class DoublePageButton extends PageButton {
#if MC_VER >= MC_1_21_11
    private static final Identifier PAGE_FORWARD_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("widget/page_forward_highlighted");
    private static final Identifier PAGE_FORWARD_SPRITE = Identifier.withDefaultNamespace("widget/page_forward");
    private static final Identifier PAGE_BACKWARD_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("widget/page_backward_highlighted");
    private static final Identifier PAGE_BACKWARD_SPRITE = Identifier.withDefaultNamespace("widget/page_backward");
#else
    private static final ResourceLocation PAGE_FORWARD_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/page_forward_highlighted");
    private static final ResourceLocation PAGE_FORWARD_SPRITE = ResourceLocation.withDefaultNamespace("widget/page_forward");
    private static final ResourceLocation PAGE_BACKWARD_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/page_backward_highlighted");
    private static final ResourceLocation PAGE_BACKWARD_SPRITE = ResourceLocation.withDefaultNamespace("widget/page_backward");
#endif

    private final boolean isForward;

    public DoublePageButton(int x, int y, boolean isForward, OnPress onPress, boolean playTurnSound) {
        super(x, y, isForward, onPress, playTurnSound);
        this.isForward = isForward;
    }

#if MC_VER >= MC_26_1
    @Override
    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        Identifier sprite;
#elif MC_VER >= MC_1_21_11
    @Override
    public void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Identifier sprite;
#else
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation sprite;
#endif

        if (this.isForward) {
            sprite = this.isHoveredOrFocused() ? PAGE_FORWARD_HIGHLIGHTED_SPRITE : PAGE_FORWARD_SPRITE;
        } else {
            sprite = this.isHoveredOrFocused() ? PAGE_BACKWARD_HIGHLIGHTED_SPRITE : PAGE_BACKWARD_SPRITE;
        }

        // Render the arrow twice to create a double arrow effect
        int offset = this.isForward ? 6 : -6;
#if MC_VER >= MC_26_1
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX() + offset, this.getY(), 23, 13);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), 23, 13);
#elif MC_VER >= MC_1_21_6
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX() + offset, this.getY(), 23, 13);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), 23, 13);
#elif MC_VER >= MC_1_21_2
        guiGraphics.blitSprite(RenderType::guiTextured, sprite, this.getX() + offset, this.getY(), 23, 13);
        guiGraphics.blitSprite(RenderType::guiTextured, sprite, this.getX(), this.getY(), 23, 13);
#else
        guiGraphics.blitSprite(sprite, this.getX() + offset, this.getY(), 23, 13);
        guiGraphics.blitSprite(sprite, this.getX(), this.getY(), 23, 13);
#endif
    }
}
