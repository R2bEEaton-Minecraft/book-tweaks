package cc.spea.booktweaks.mixin.client;

import cc.spea.booktweaks.PageMemoryManager;
import cc.spea.booktweaks.client.DoublePageButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
    private MultiLineEditBox page;

    @Shadow
    private PageButton forwardButton;

    @Shadow
    private PageButton backButton;

    @Shadow
    protected abstract void updateButtonVisibility();

    @Shadow
    protected abstract void updatePageContent();

    @Shadow
    protected abstract int getNumPages();

    @Shadow
    protected abstract void pageForward();

    @Shadow
    protected abstract void pageBack();

    @Unique
    private boolean bookTweaks$initialPageSet = false;

    @Unique
    private PageButton bookTweaks$jumpToStartButton;

    @Unique
    private PageButton bookTweaks$jumpToEndButton;

    /**
     * Inject into init method to add our custom buttons and set initial page.
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        BookEditScreen screen = (BookEditScreen) (Object) this;
        ScreenAccessor accessor = (ScreenAccessor) this;

        int centerX = (screen.width - 192) / 2;
        int buttonY = 159;
        int buttonSpacing = 5;

        // Reposition existing buttons to make room for jump buttons
        // Move 8 pixels to the right from original position
        int startX = centerX + 38;

        // Move existing back button
        backButton.setX(startX + 23 + buttonSpacing);

        // Move existing forward button
        forwardButton.setX(startX + (23 + buttonSpacing) * 2);

        // Add "Jump to Start" button (double left arrow)
        bookTweaks$jumpToStartButton = accessor.invokeAddRenderableWidget(
                new DoublePageButton(startX, buttonY, false, button -> bookTweaks$jumpToStart(), true)
        );

        // Add "Jump to End" button (double right arrow)
        bookTweaks$jumpToEndButton = accessor.invokeAddRenderableWidget(
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
     * Writeable books default to page 1 if no page is remembered.
     */
    @Unique
    private void bookTweaks$setInitialPage() {
        Integer rememberedPage = PageMemoryManager.getRememberedPage(book);

        if (rememberedPage != null) {
            // Jump to remembered page
            currentPage = Math.max(0, Math.min(rememberedPage, getNumPages() - 1));
        } else {
            // Default to first page for writeable books
            currentPage = 0;
        }

        updatePageContent();
        updateButtonVisibility();
    }

    /**
     * Jump to the first page.
     */
    @Unique
    private void bookTweaks$jumpToStart() {
        currentPage = 0;
        updatePageContent();
        updateButtonVisibility();
    }

    /**
     * Jump to the last page.
     */
    @Unique
    private void bookTweaks$jumpToEnd() {
        currentPage = Math.max(0, getNumPages() - 1);
        updatePageContent();
        updateButtonVisibility();
    }

    /**
     * Update jump button visibility based on current page.
     */
    @Inject(method = "updateButtonVisibility", at = @At("TAIL"))
    private void updateJumpButtonVisibility(CallbackInfo ci) {
        if (bookTweaks$jumpToStartButton != null) {
            bookTweaks$jumpToStartButton.visible = currentPage > 0;
        }
        if (bookTweaks$jumpToEndButton != null) {
            bookTweaks$jumpToEndButton.visible = currentPage < getNumPages() - 1;
        }
    }

    /**
     * Remember the current page when the book is closed/saved.
     * Inject at TAIL to ensure the ItemStack is updated with new content first.
     */
    @Inject(method = "saveChanges", at = @At("TAIL"))
    private void onSave(CallbackInfo ci) {
        PageMemoryManager.rememberPage(book, currentPage);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = false)
    private void onKeyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> ci) {
        MultiLineEditBoxMixin mleb = (MultiLineEditBoxMixin) (Object) this.page;
        MultilineTextFieldMixin mltfaccessor = (MultilineTextFieldMixin) (Object) mleb.bookTweaks$getTextField();
        if (mltfaccessor.bookTweaks$overflowsLineLimit(this.page.getValue() + "_")) {
            if (keyEvent.isPaste()) {
                System.out.println(insertWouldOverflow(Minecraft.getInstance().keyboardHandler.getClipboard(), mltfaccessor));
                return;
            } else if (keyEvent.isCut()) {
                System.out.println(insertWouldOverflow("", mltfaccessor));
                return;
            } else {
                switch (keyEvent.key()) {
                    case 257:
                    case 335:
                        System.out.println(insertWouldOverflow("\n", mltfaccessor));
                        return;
                    // case 262:
                    //     if (keyEvent.hasControlDown()) {
                    //         MultilineTextField.StringView stringView = this.getNextWord();
                    //         this.seekCursor(Whence.ABSOLUTE, stringView.beginIndex);
                    //     } else {
                    //         this.seekCursor(Whence.RELATIVE, 1);
                    //     }

                    //     return true;
                    // case 263:
                    //     if (keyEvent.hasControlDown()) {
                    //         MultilineTextField.StringView stringView = this.getPreviousWord();
                    //         this.seekCursor(Whence.ABSOLUTE, stringView.beginIndex);
                    //     } else {
                    //         this.seekCursor(Whence.RELATIVE, -1);
                    //     }

                    //     return true;
                    // case 264:
                    //     if (!keyEvent.hasControlDown()) {
                    //         this.seekCursorLine(1);
                    //     }

                    //     return true;
                    // case 265:
                    //     if (!keyEvent.hasControlDown()) {
                    //         this.seekCursorLine(-1);
                    //     }

                    //     return true;
                    // case 266:
                    //     this.seekCursor(Whence.ABSOLUTE, 0);
                    //     return true;
                    // case 267:
                    //     this.seekCursor(Whence.END, 0);
                    //     return true;
                    // case 268:
                    //     if (keyEvent.hasControlDown()) {
                    //         this.seekCursor(Whence.ABSOLUTE, 0);
                    //     } else {
                    //         this.seekCursor(Whence.ABSOLUTE, this.getCursorLineView().beginIndex);
                    //     }

                    //     return true;
                    // case 269:
                    //     if (keyEvent.hasControlDown()) {
                    //         this.seekCursor(Whence.END, 0);
                    //     } else {
                    //         this.seekCursor(Whence.ABSOLUTE, this.getCursorLineView().endIndex);
                    //     }

                    //     return true;
                    default:
                        System.out.println("default case");
                        return;
                }
            }
        }
    }

    @Unique
    private boolean insertWouldOverflow(String string, MultilineTextFieldMixin mltfaccessor) {
        if (!string.isEmpty() || mltfaccessor.bookTweaks$hasSelection()) {
			String string2 = mltfaccessor.bookTweaks$truncateInsertionText(StringUtil.filterText(string, true));
            int beginIndex = Math.min(mltfaccessor.selectCursor, mltfaccessor.cursor);
            int endIndex = Math.max(mltfaccessor.selectCursor, mltfaccessor.cursor);
			String string3 = new StringBuilder(mltfaccessor.value).replace(beginIndex, endIndex, string2).toString();
			return mltfaccessor.bookTweaks$overflowsLineLimit(string3);
		}
        return false;
    }
}