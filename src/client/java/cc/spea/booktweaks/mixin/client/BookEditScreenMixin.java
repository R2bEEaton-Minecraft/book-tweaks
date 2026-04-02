package cc.spea.booktweaks.mixin.client;

import cc.spea.booktweaks.PageMemoryManager;
#if MC_VER >= MC_1_21_6
import cc.spea.booktweaks.accessor.MultiLineEditBoxAccessor;
import cc.spea.booktweaks.accessor.MultilineTextFieldAccessor;
#endif
import cc.spea.booktweaks.client.DoublePageButton;
import net.minecraft.client.Minecraft;
#if MC_VER >= MC_1_21_6
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.Whence;
#else
import net.minecraft.client.gui.font.TextFieldHelper;
#endif
#if MC_VER < MC_1_21_9
import net.minecraft.client.gui.screens.Screen;
#endif
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
#if MC_VER >= MC_1_21_9
import net.minecraft.client.input.KeyEvent;
#endif
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

import java.lang.reflect.Field;
import java.util.Arrays;
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

#if MC_VER >= MC_1_21_6
    @Shadow
    private MultiLineEditBox page;
#else
    @Shadow
    @Final
    private TextFieldHelper pageEdit;
#endif

    @Shadow
    private PageButton forwardButton;

    @Shadow
    private PageButton backButton;

    @Shadow
    protected abstract void updateButtonVisibility();

    @Shadow
    protected abstract int getNumPages();

    @Shadow
    protected abstract void pageForward();

    @Shadow
    protected abstract void pageBack();

#if MC_VER >= MC_1_21_6
    @Shadow
    protected abstract void saveChanges();
#else
    @Shadow
    protected abstract String getCurrentPageText();

    @Shadow
    protected abstract void setCurrentPageText(String string);

    @Shadow
    protected abstract void saveChanges(boolean bl);
#endif

    @Unique
    private boolean bookTweaks$initialPageSet = false;

    @Unique
    private PageButton bookTweaks$jumpToStartButton;

    @Unique
    private PageButton bookTweaks$jumpToEndButton;

#if MC_VER < MC_1_21_6
    @Unique
    private static Field bookTweaks$legacyDisplayCacheField;

    @Unique
    private static Field bookTweaks$legacyLineStartsField;

    @Unique
    private static Field bookTweaks$legacyLinesField;
#endif

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
        bookTweaks$goToPage(rememberedPage != null ? rememberedPage : 0);
    }

    /**
     * Jump to the first page.
     */
    @Unique
    private void bookTweaks$jumpToStart() {
        bookTweaks$goToPage(0);
    }

    /**
     * Jump to the last page.
     */
    @Unique
    private void bookTweaks$jumpToEnd() {
        bookTweaks$goToPage(getNumPages() - 1);
    }

    @Unique
    private void bookTweaks$goToPage(int targetPage) {
        int clampedTarget = Math.max(0, Math.min(targetPage, getNumPages() - 1));

        while (currentPage < clampedTarget) {
            pageForward();
        }
        while (currentPage > clampedTarget) {
            pageBack();
        }

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
     * Persist page memory immediately when page navigation succeeds, even if no text changed.
     */
    @Inject(method = "pageBack", at = @At("TAIL"))
    private void onPageBackRemember(CallbackInfo ci) {
        PageMemoryManager.rememberPage(book, currentPage);
    }

    /**
     * Persist page memory immediately when page navigation succeeds, even if no text changed.
     */
    @Inject(method = "pageForward", at = @At("TAIL"))
    private void onPageForwardRemember(CallbackInfo ci) {
        PageMemoryManager.rememberPage(book, currentPage);
    }

    /**
     * Remember the current page when the book is closed/saved.
     * Inject at TAIL to ensure the ItemStack is updated with new content first.
     */
#if MC_VER >= MC_1_21_6
    @Inject(method = "saveChanges", at = @At("TAIL"))
    private void onSave(CallbackInfo ci) {
        PageMemoryManager.rememberPage(book, currentPage);
    }
#else
    @Inject(method = "saveChanges", at = @At("TAIL"))
    private void onSave(boolean bl, CallbackInfo ci) {
        PageMemoryManager.rememberPage(book, currentPage);
    }
#endif

    /**
     * Handle key presses to manage page overflow on insertions.
     */
#if MC_VER >= MC_1_21_9
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> ci) {
        bookTweaks$handleKeyPressed(keyEvent.key(), keyEvent.isPaste(), ci);
    }
#else
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> ci) {
        bookTweaks$handleKeyPressed(keyCode, Screen.isPaste(keyCode), ci);
    }
#endif

    @Unique
    private void bookTweaks$handleKeyPressed(int keyCode, boolean isPaste, CallbackInfoReturnable<Boolean> ci) {
#if MC_VER >= MC_1_21_6
        MultiLineEditBoxAccessor mleb = (MultiLineEditBoxAccessor) this.page;
        MultilineTextFieldAccessor mltfaccessor = (MultilineTextFieldAccessor) mleb.bookTweaks$getTextField();
        if (isPaste) {
            String remaining = Minecraft.getInstance().keyboardHandler.getClipboard();

            while (!remaining.isEmpty()) {
                String toPaste = remaining;

                while (insertWouldOverflow(toPaste, mltfaccessor) && !toPaste.isEmpty()) {
                    int lastSpace = toPaste.lastIndexOf(' ');
                    if (lastSpace <= 0) {
                        break;
                    }
                    toPaste = toPaste.substring(0, lastSpace);
                }

                if (toPaste.isEmpty()) {
                    if (currentPage < getNumPages() - 1) {
                        pageForward();
                        mleb = (MultiLineEditBoxAccessor) this.page;
                        mltfaccessor = (MultilineTextFieldAccessor) mleb.bookTweaks$getTextField();
                        continue;
                    }
                    break;
                }

                mltfaccessor.bookTweaks$insertText(toPaste);
                remaining = remaining.substring(toPaste.length()).trim();

                if (!remaining.isEmpty()) {
                    pageForward();
                    mleb = (MultiLineEditBoxAccessor) this.page;
                    mltfaccessor = (MultilineTextFieldAccessor) mleb.bookTweaks$getTextField();
                }
            }

            ci.setReturnValue(true);
            return;
        }

        switch (keyCode) {
            case 256:
                bookTweaks$saveChangesForCurrentVersion();
                PageMemoryManager.rememberPage(book, currentPage);
                return;
            case 257:
            case 335:
                if (insertWouldOverflow("\n", mltfaccessor)
                        && mltfaccessor.bookTweaks$getLineAtCursor() == 126 / 9 - 1
                        && mltfaccessor.bookTweaks$getCursor() == this.page.getValue().length()) {
                    pageForward();
                }
                return;
            case 262:
                if (mltfaccessor.bookTweaks$getLineAtCursor() == 126 / 9 - 1
                        && mltfaccessor.bookTweaks$getCursor() == this.page.getValue().length()) {
                    pageForward();
                    mltfaccessor.bookTweaks$seekCursor(Whence.ABSOLUTE, 0);
                    ci.setReturnValue(true);
                }
                return;
            case 263:
                if (mltfaccessor.bookTweaks$getCursor() == 0 && currentPage > 0) {
                    pageBack();
                    mltfaccessor.bookTweaks$seekCursor(Whence.END, 0);
                    ci.setReturnValue(true);
                }
                return;
            case 264:
                if (mltfaccessor.bookTweaks$getLineAtCursor() == 126 / 9 - 1) {
                    pageForward();
                    mltfaccessor.bookTweaks$seekCursor(Whence.END, 0);
                    ci.setReturnValue(true);
                }
                return;
            case 265:
                if (mltfaccessor.bookTweaks$getLineAtCursor() == 0 && currentPage > 0) {
                    pageBack();
                    mltfaccessor.bookTweaks$seekCursor(Whence.END, 0);
                    ci.setReturnValue(true);
                }
                return;
            case 266:
            case 267:
            case 268:
            case 269:
                return;
            case 259:
                if (this.page.getValue().isEmpty()) {
                    pageBack();
                    ci.setReturnValue(true);
                }
                return;
            default:
                if (keyCode >= 340 && keyCode <= 348) {
                    return;
                }

                if (insertWouldOverflow("a", mltfaccessor)
                        && mltfaccessor.bookTweaks$getLineAtCursor() == 126 / 9 - 1
                        && mltfaccessor.bookTweaks$getCursor() == this.page.getValue().length()) {
                    if (currentPage == getNumPages() - 1 || pages.get(currentPage + 1).isEmpty()) {
                        int beginIndex = mltfaccessor.bookTweaks$getPreviousWordBeginIndex();
                        int endIndex = mltfaccessor.bookTweaks$getPreviousWordEndIndex();
                        int wordLength = endIndex - beginIndex;

                        if (mltfaccessor.bookTweaks$getCursor() == endIndex && wordLength <= 16) {
                            String wordToMove = this.page.getValue().substring(beginIndex, endIndex);
                            this.page.setValue(this.page.getValue().substring(0, beginIndex));
                            pageForward();
                            this.page.setValue(wordToMove + this.page.getValue());
                        } else {
                            pageForward();
                        }
                    }
                }
                return;
        }
#else
        if (isPaste) {
            String remaining = Minecraft.getInstance().keyboardHandler.getClipboard();

            while (!remaining.isEmpty()) {
                String toPaste = remaining;

                while (bookTweaks$legacyInsertWouldOverflow(toPaste) && !toPaste.isEmpty()) {
                    int lastSpace = toPaste.lastIndexOf(' ');
                    if (lastSpace <= 0) {
                        break;
                    }
                    toPaste = toPaste.substring(0, lastSpace);
                }

                if (toPaste.isEmpty()) {
                    if (currentPage < getNumPages() - 1) {
                        pageForward();
                        pageEdit.setCursorToStart();
                        continue;
                    }
                    break;
                }

                pageEdit.insertText(toPaste);
                remaining = remaining.substring(toPaste.length()).trim();

                if (!remaining.isEmpty()) {
                    pageForward();
                    pageEdit.setCursorToStart();
                }
            }

            ci.setReturnValue(true);
            return;
        }

        String currentText = getCurrentPageText();
        int cursor = pageEdit.getCursorPos();
        int lineAtCursor = bookTweaks$getLegacyLineAtCursor();
        int lastLine = bookTweaks$getLegacyLineCount() - 1;

        switch (keyCode) {
            case 256:
                bookTweaks$saveChangesForCurrentVersion();
                PageMemoryManager.rememberPage(book, currentPage);
                return;
            case 257:
            case 335:
                if (bookTweaks$legacyInsertWouldOverflow("\n") && lineAtCursor == lastLine && cursor == currentText.length()) {
                    pageForward();
                    pageEdit.setCursorToStart();
                }
                return;
            case 262:
                if (lineAtCursor == lastLine && cursor == currentText.length()) {
                    pageForward();
                    pageEdit.setCursorToStart();
                    ci.setReturnValue(true);
                }
                return;
            case 263:
                if (cursor == 0 && currentPage > 0) {
                    pageBack();
                    pageEdit.setCursorToEnd();
                    ci.setReturnValue(true);
                }
                return;
            case 264:
                if (lineAtCursor == lastLine) {
                    pageForward();
                    pageEdit.setCursorToEnd();
                    ci.setReturnValue(true);
                }
                return;
            case 265:
                if (lineAtCursor == 0 && currentPage > 0) {
                    pageBack();
                    pageEdit.setCursorToEnd();
                    ci.setReturnValue(true);
                }
                return;
            case 266:
            case 267:
            case 268:
            case 269:
                return;
            case 259:
                if (currentText.isEmpty()) {
                    pageBack();
                    ci.setReturnValue(true);
                }
                return;
            default:
                if (keyCode >= 340 && keyCode <= 348) {
                    return;
                }

                if (bookTweaks$legacyInsertWouldOverflow("a") && lineAtCursor == lastLine && cursor == currentText.length()) {
                    if (currentPage == getNumPages() - 1 || pages.get(currentPage + 1).isEmpty()) {
                        int beginIndex = bookTweaks$getLegacyPreviousWordBeginIndex(currentText, cursor);
                        int endIndex = bookTweaks$getLegacyPreviousWordEndIndex(currentText, cursor);
                        int wordLength = endIndex - beginIndex;

                        if (cursor == endIndex && wordLength <= 16) {
                            String wordToMove = currentText.substring(beginIndex, endIndex);
                            setCurrentPageText(currentText.substring(0, beginIndex));
                            pageForward();
                            pageEdit.setCursorToStart();
                            pageEdit.insertText(wordToMove);
                        } else {
                            pageForward();
                            pageEdit.setCursorToStart();
                        }
                    }
                }
                return;
        }
#endif
    }

#if MC_VER >= MC_1_21_6
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
#else
    @Unique
    private boolean bookTweaks$legacyInsertWouldOverflow(String string) {
        String filtered = StringUtil.filterText(string, true);
        int beginIndex = Math.min(pageEdit.getSelectionPos(), pageEdit.getCursorPos());
        int endIndex = Math.max(pageEdit.getSelectionPos(), pageEdit.getCursorPos());
        String candidate = new StringBuilder(getCurrentPageText()).replace(beginIndex, endIndex, filtered).toString();
        return candidate.length() > 1024 || Minecraft.getInstance().font.wordWrapHeight(candidate, 114) > 128;
    }

    @Unique
    private int bookTweaks$getLegacyLineAtCursor() {
        int cursor = pageEdit.getCursorPos();
        int[] lineStarts = bookTweaks$getLegacyLineStarts();
        int line = Arrays.binarySearch(lineStarts, cursor);
        if (line < 0) {
            line = -line - 2;
        }
        return Math.max(0, line);
    }

    @Unique
    private int bookTweaks$getLegacyLineCount() {
        try {
            Object[] lines = (Object[]) bookTweaks$getLegacyLinesField().get(bookTweaks$getLegacyDisplayCache());
            return Math.max(1, lines.length);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to read BookEditScreen display lines", e);
        }
    }

    @Unique
    private int[] bookTweaks$getLegacyLineStarts() {
        try {
            return (int[]) bookTweaks$getLegacyLineStartsField().get(bookTweaks$getLegacyDisplayCache());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to read BookEditScreen line starts", e);
        }
    }

    @Unique
    private Object bookTweaks$getLegacyDisplayCache() {
        try {
            return bookTweaks$getLegacyDisplayCacheField().get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to read BookEditScreen display cache", e);
        }
    }

    @Unique
    private Field bookTweaks$getLegacyDisplayCacheField() {
        if (bookTweaks$legacyDisplayCacheField == null) {
            try {
                bookTweaks$legacyDisplayCacheField = this.getClass().getDeclaredField("displayCache");
                bookTweaks$legacyDisplayCacheField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("Failed to locate BookEditScreen displayCache field", e);
            }
        }
        return bookTweaks$legacyDisplayCacheField;
    }

    @Unique
    private Field bookTweaks$getLegacyLineStartsField() {
        if (bookTweaks$legacyLineStartsField == null) {
            try {
                bookTweaks$legacyLineStartsField = bookTweaks$getLegacyDisplayCache().getClass().getDeclaredField("lineStarts");
                bookTweaks$legacyLineStartsField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("Failed to locate BookEditScreen lineStarts field", e);
            }
        }
        return bookTweaks$legacyLineStartsField;
    }

    @Unique
    private Field bookTweaks$getLegacyLinesField() {
        if (bookTweaks$legacyLinesField == null) {
            try {
                bookTweaks$legacyLinesField = bookTweaks$getLegacyDisplayCache().getClass().getDeclaredField("lines");
                bookTweaks$legacyLinesField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("Failed to locate BookEditScreen lines field", e);
            }
        }
        return bookTweaks$legacyLinesField;
    }

    @Unique
    private int bookTweaks$getLegacyPreviousWordBeginIndex(String value, int cursor) {
        if (value.isEmpty()) {
            return 0;
        }

        int i = Math.max(0, Math.min(cursor, value.length() - 1));
        while (i > 0 && Character.isWhitespace(value.charAt(i - 1))) {
            i--;
        }
        while (i > 0 && !Character.isWhitespace(value.charAt(i - 1))) {
            i--;
        }
        return i;
    }

    @Unique
    private int bookTweaks$getLegacyPreviousWordEndIndex(String value, int cursor) {
        if (value.isEmpty()) {
            return 0;
        }

        int i = bookTweaks$getLegacyPreviousWordBeginIndex(value, cursor);
        while (i < value.length() && !Character.isWhitespace(value.charAt(i))) {
            i++;
        }
        return i;
    }
#endif

    @Unique
    private void bookTweaks$saveChangesForCurrentVersion() {
#if MC_VER >= MC_1_21_6
        saveChanges();
#else
        saveChanges(false);
#endif
    }
}
