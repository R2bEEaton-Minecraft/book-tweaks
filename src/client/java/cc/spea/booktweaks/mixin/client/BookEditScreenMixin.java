package cc.spea.booktweaks.mixin.client;

import cc.spea.booktweaks.PageMemoryManager;
import cc.spea.booktweaks.accessor.MultiLineEditBoxAccessor;
import cc.spea.booktweaks.accessor.MultilineTextFieldAccessor;
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
    /**
     * Handle key presses to manage page overflow on insertions.
     */
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> ci) {
        MultiLineEditBoxAccessor mleb = (MultiLineEditBoxAccessor) this.page;
        MultilineTextFieldAccessor mltfaccessor = (MultilineTextFieldAccessor) mleb.bookTweaks$getTextField();
            if (keyEvent.isPaste()) {
                String clipboardContent = Minecraft.getInstance().keyboardHandler.getClipboard();
                String remaining = clipboardContent;

                while (!remaining.isEmpty()) {
                    // Try to paste as much as possible on current page
                    String toPaste = remaining;

                    // If it would overflow, trim to last word boundary
                    while (insertWouldOverflow(toPaste, mltfaccessor) && toPaste.length() > 0) {
                        int lastSpace = toPaste.lastIndexOf(' ');
                        if (lastSpace <= 0) {
                            // No space found, can't fit anything more
                            break;
                        }
                        toPaste = toPaste.substring(0, lastSpace);
                    }

                    if (toPaste.isEmpty()) {
                        // Nothing fits on this page, move to next
                        if (this.currentPage < this.getNumPages() - 1) {
                            this.pageForward();
                            mleb = (MultiLineEditBoxAccessor) this.page;
                            mltfaccessor = (MultilineTextFieldAccessor) mleb.bookTweaks$getTextField();
                            continue;
                        } else {
                            // Last page and nothing fits, we're done
                            break;
                        }
                    }

                    // Actually insert the text on current page
                    mltfaccessor.bookTweaks$insertText(toPaste);
                    remaining = remaining.substring(toPaste.length()).trim();

                    // If there's more to paste, move to next page
                    if (!remaining.isEmpty()) {
                        if (this.currentPage < this.getNumPages() - 1) {
                            this.pageForward();
                        } else {
                            // Create new page by moving forward (which adds a page if needed)
                            this.pageForward();
                        }
                        mleb = (MultiLineEditBoxAccessor) this.page;
                        mltfaccessor = (MultilineTextFieldAccessor) mleb.bookTweaks$getTextField();
                    }
                }

                ci.setReturnValue(true);
                return;
            } else {
                switch (keyEvent.key()) {
                    case 257:
                    case 335:
                        if (insertWouldOverflow("\n", mltfaccessor) && mltfaccessor.bookTweaks$getLineAtCursor() == 126 / 9 - 1 && mltfaccessor.bookTweaks$getCursor() == this.page.getValue().length()) {
                            System.out.println("newline overflowed, moving to next page");
                            this.pageForward();
                        }
                        return;
                    case 262:
                        System.out.println("right arrow pressed");
                        System.out.println("cursor at: " + mltfaccessor.bookTweaks$getCursor());
                        System.out.println("page length: " + this.page.getValue().length());
                        if (mltfaccessor.bookTweaks$getLineAtCursor() == 126 / 9 - 1 && mltfaccessor.bookTweaks$getCursor() == this.page.getValue().length()) {
                            this.pageForward();
                            mltfaccessor.bookTweaks$seekCursor(Whence.ABSOLUTE, 0);
                        }
					    return;
                    case 263:
                        if (mltfaccessor.bookTweaks$getCursor() == 0 && this.currentPage > 0) {
                            this.pageBack();
                            mltfaccessor.bookTweaks$seekCursor(Whence.END, 0);
                            ci.setReturnValue(true);
                        }
					    return;
                    case 264:
                        if (mltfaccessor.bookTweaks$getLineAtCursor() == 126 / 9 - 1) {
                            this.pageForward();
                            mltfaccessor.bookTweaks$seekCursor(Whence.END, 0);
                            ci.setReturnValue(true);
                        }
                        return;
                    case 265:
                        if (mltfaccessor.bookTweaks$getLineAtCursor() == 0 && this.currentPage > 0) {
                            this.pageBack();
                            mltfaccessor.bookTweaks$seekCursor(Whence.END, 0);
                            ci.setReturnValue(true);
                        }
					    return;
                    case 266:
                    case 267:
                    case 268:
                    case 269:
                        System.out.println("seek attempted " + keyEvent.key());
                        return;
                    case 259:
                        System.out.println("deletion attempted");
                        System.out.println(this.page.getValue());
                        if (this.page.getValue().isEmpty()) {
                            this.pageBack();
                            ci.setReturnValue(true);
                        }
                        return;
                    default:
                        // Only handle actual character input, not control keys
                        // Control keys: Shift (340-341), Ctrl (342-343), Alt (344-345), Super (347-348), etc.
                        int key = keyEvent.key();
                        if (key >= 340 && key <= 348) {
                            // Ignore modifier keys
                            return;
                        }

                        System.out.println("default case " + key);
                        // Check if inserting a character would overflow
                        // We use a single character placeholder to test overflow
                        if (insertWouldOverflow("a", mltfaccessor) && mltfaccessor.bookTweaks$getLineAtCursor() == 126 / 9 - 1 && mltfaccessor.bookTweaks$getCursor() == this.page.getValue().length()) {
                            System.out.println("insertion overflowed, moving to next page");
                            if (this.currentPage == this.getNumPages() - 1 || this.pages.get(this.currentPage + 1).isEmpty()) {
                                int beginIndex = mltfaccessor.bookTweaks$getPreviousWordBeginIndex();
                                int endIndex = mltfaccessor.bookTweaks$getPreviousWordEndIndex();
                                int wordLength = endIndex - beginIndex;

                                if (mltfaccessor.bookTweaks$getCursor() == endIndex && wordLength <= 16) {
                                    // Cursor is at the end of a word and word is 16 chars or less, move entire word to next page
                                    String wordToMove = this.page.getValue().substring(beginIndex, endIndex);
                                    this.page.setValue(this.page.getValue().substring(0, beginIndex));
                                    this.pageForward();
                                    this.page.setValue(wordToMove + this.page.getValue());
                                } else {
                                    // Word too long or cursor not at end, just move to next page
                                    this.pageForward();
                                }
                            }
                        }
                        return;
                }
            }
    }

    @Unique
    private boolean insertWouldOverflow(String string, MultilineTextFieldAccessor mltfaccessor) {
        if (!string.isEmpty() || mltfaccessor.bookTweaks$hasSelection()) {
			String string2 = mltfaccessor.bookTweaks$truncateInsertionText(StringUtil.filterText(string, true));
            int beginIndex = Math.min(mltfaccessor.bookTweaks$getSelectCursor(), mltfaccessor.bookTweaks$getCursor());
            int endIndex = Math.max(mltfaccessor.bookTweaks$getSelectCursor(), mltfaccessor.bookTweaks$getCursor());
			String string3 = new StringBuilder(mltfaccessor.bookTweaks$getValue()).replace(beginIndex, endIndex, string2).toString();
			return mltfaccessor.bookTweaks$overflowsLineLimit(string3);
		}
        return false;
    }
}