package cc.spea.booktweaks.mixin.client;

import cc.spea.booktweaks.PageMemoryManager;
import cc.spea.booktweaks.client.DoublePageButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BookViewScreen.class)
public abstract class BookScreenMixin {
    @Shadow
    private int currentPage;

    @Shadow
    @Final
    private BookViewScreen.BookAccess bookAccess;

    @Shadow
    private PageButton forwardButton;

    @Shadow
    private PageButton backButton;

    @Shadow
    protected abstract void updateButtonVisibility();

    @Shadow
    public abstract boolean setPage(int i);

    @Unique
    private ItemStack bookTweaks$bookStack;

    @Unique
    private boolean bookTweaks$initialPageSet = false;

    /**
     * Capture the book ItemStack when the screen is constructed with a book.
     */
    @Inject(method = "<init>(Lnet/minecraft/client/gui/screens/inventory/BookViewScreen$BookAccess;)V", at = @At("TAIL"))
    private void onConstruct(BookViewScreen.BookAccess bookAccess, CallbackInfo ci) {
        // We'll try to get the ItemStack from the player's hand
        // This is a limitation - we store it when we can access it
    }

    /**
     * Inject into init method to add our custom buttons and set initial page.
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        BookViewScreen screen = (BookViewScreen) (Object) this;
        ScreenAccessor accessor = (ScreenAccessor) this;

        // Try to get the book from player's hand
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            ItemStack mainHand = mc.player.getMainHandItem();
            ItemStack offHand = mc.player.getOffhandItem();

            // Check which hand has a book
            if (mainHand != null && BookViewScreen.BookAccess.fromItem(mainHand) != null) {
                bookTweaks$bookStack = mainHand;
            } else if (offHand != null && BookViewScreen.BookAccess.fromItem(offHand) != null) {
                bookTweaks$bookStack = offHand;
            }
        }

        int centerX = (screen.width - 192) / 2;
        int buttonY = 159;
        int buttonSpacing = 5;

        // Reposition existing buttons to make room for jump buttons
        int startX = centerX + 30;

        // Move existing back button
        backButton.setX(startX + 23 + buttonSpacing);

        // Move existing forward button
        forwardButton.setX(startX + (23 + buttonSpacing) * 2);

        // Add "Jump to Start" button (double left arrow)
        accessor.invokeAddRenderableWidget(
                new DoublePageButton(startX, buttonY, false, button -> bookTweaks$jumpToStart(), true)
        );

        // Add "Jump to End" button (double right arrow)
        accessor.invokeAddRenderableWidget(
                new DoublePageButton(startX + (23 + buttonSpacing) * 3, buttonY, true, button -> bookTweaks$jumpToEnd(), true)
        );

        // Set initial page if not already set
        if (!bookTweaks$initialPageSet) {
            bookTweaks$setInitialPage();
            bookTweaks$initialPageSet = true;
        }
    }

    /**
     * Set the initial page when opening the book.
     * Written books default to the beginning if no page is remembered.
     */
    @Unique
    private void bookTweaks$setInitialPage() {
        if (bookTweaks$bookStack != null) {
            Integer rememberedPage = PageMemoryManager.getRememberedPage(bookTweaks$bookStack);

            if (rememberedPage != null) {
                // Jump to remembered page
                setPage(rememberedPage);
                return;
            }
        }

        // Default to first page for written books
        setPage(0);
    }

    /**
     * Jump to the first page.
     */
    @Unique
    private void bookTweaks$jumpToStart() {
        setPage(0);
    }

    /**
     * Jump to the last page.
     */
    @Unique
    private void bookTweaks$jumpToEnd() {
        setPage(bookAccess.getPageCount() - 1);
    }

    /**
     * Remember the current page when navigating backward.
     */
    @Inject(method = "pageBack", at = @At("HEAD"))
    private void onPageBack(CallbackInfo ci) {
        if (bookTweaks$bookStack != null) {
            PageMemoryManager.rememberPage(bookTweaks$bookStack, currentPage);
        }
    }

    /**
     * Remember the current page when navigating forward.
     */
    @Inject(method = "pageForward", at = @At("HEAD"))
    private void onPageForward(CallbackInfo ci) {
        if (bookTweaks$bookStack != null) {
            PageMemoryManager.rememberPage(bookTweaks$bookStack, currentPage);
        }
    }

    /**
     * Remember the current page when rendering (saves periodically).
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        if (bookTweaks$bookStack != null) {
            PageMemoryManager.rememberPage(bookTweaks$bookStack, currentPage);
        }
    }
}
