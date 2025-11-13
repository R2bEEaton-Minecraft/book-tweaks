package cc.spea.booktweaks.mixin.client;

import cc.spea.booktweaks.PageMemoryManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
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
    protected abstract void updateButtonVisibility();

    @Shadow
    protected abstract <T extends net.minecraft.client.gui.components.events.GuiEventListener & net.minecraft.client.gui.components.Renderable & net.minecraft.client.gui.narration.NarratableEntry> T addRenderableWidget(T widget);

    @Shadow
    public net.minecraft.client.Minecraft minecraft;

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

        // Try to get the book from player's hand
        if (minecraft != null && minecraft.player != null) {
            ItemStack mainHand = minecraft.player.getMainHandItem();
            ItemStack offHand = minecraft.player.getOffhandItem();

            // Check which hand has a book
            if (mainHand != null && BookViewScreen.BookAccess.fromItem(mainHand) != null) {
                bookTweaks$bookStack = mainHand;
            } else if (offHand != null && BookViewScreen.BookAccess.fromItem(offHand) != null) {
                bookTweaks$bookStack = offHand;
            }
        }

        // Add "Jump to Start" button (left side)
        addRenderableWidget(
                Button.builder(Component.literal("<<"), button -> bookTweaks$jumpToStart())
                        .bounds(screen.width / 2 - 100 - 50, 196, 20, 20)
                        .build()
        );

        // Add "Jump to End" button (right side)
        addRenderableWidget(
                Button.builder(Component.literal(">>"), button -> bookTweaks$jumpToEnd())
                        .bounds(screen.width / 2 + 100 + 30, 196, 20, 20)
                        .build()
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
                currentPage = Math.max(0, Math.min(rememberedPage, bookAccess.getPageCount() - 1));
                updateButtonVisibility();
                return;
            }
        }

        // Default to first page for written books
        currentPage = 0;
        updateButtonVisibility();
    }

    /**
     * Jump to the first page.
     */
    @Unique
    private void bookTweaks$jumpToStart() {
        currentPage = 0;
        updateButtonVisibility();
    }

    /**
     * Jump to the last page.
     */
    @Unique
    private void bookTweaks$jumpToEnd() {
        currentPage = Math.max(0, bookAccess.getPageCount() - 1);
        updateButtonVisibility();
    }

    /**
     * Remember the current page when the book is closed.
     */
    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        if (bookTweaks$bookStack != null) {
            PageMemoryManager.rememberPage(bookTweaks$bookStack, currentPage);
        }
    }
}
