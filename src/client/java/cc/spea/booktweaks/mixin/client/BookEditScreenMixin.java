package cc.spea.booktweaks.mixin.client;

import cc.spea.booktweaks.PageMemoryManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin {
    @Shadow
    private int currentPage;

    @Shadow
    @Final
    private ItemStack book;

    @Shadow
    @Final
    private List<String> pages;

    @Shadow
    protected abstract void updateButtonVisibility();

    @Shadow
    protected abstract int getNumPages();

    @Unique
    private boolean bookTweaks$initialPageSet = false;

    /**
     * Inject into init method to add our custom buttons and set initial page.
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        BookEditScreen screen = (BookEditScreen) (Object) this;
        ScreenAccessor accessor = (ScreenAccessor) this;

        // Add "Jump to Start" button (left side)
        accessor.invokeAddRenderableWidget(
                Button.builder(Component.literal("<<"), button -> bookTweaks$jumpToStart())
                        .bounds(screen.width / 2 - 100 - 50, 196, 20, 20)
                        .build()
        );

        // Add "Jump to End" button (right side)
        accessor.invokeAddRenderableWidget(
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
     * Writeable books default to the end if no page is remembered.
     */
    @Unique
    private void bookTweaks$setInitialPage() {
        Integer rememberedPage = PageMemoryManager.getRememberedPage(book);

        if (rememberedPage != null) {
            // Jump to remembered page
            currentPage = Math.max(0, Math.min(rememberedPage, getNumPages() - 1));
        } else {
            // Default to last page for writeable books
            currentPage = Math.max(0, getNumPages() - 1);
        }

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
        currentPage = Math.max(0, getNumPages() - 1);
        updateButtonVisibility();
    }

    /**
     * Remember the current page when the book is closed/saved.
     */
    @Inject(method = "saveChanges", at = @At("HEAD"))
    private void onSave(CallbackInfo ci) {
        PageMemoryManager.rememberPage(book, currentPage);
    }
}
