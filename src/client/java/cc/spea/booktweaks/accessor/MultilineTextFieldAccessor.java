package cc.spea.booktweaks.accessor;

import net.minecraft.client.gui.components.Whence;

/**
 * Duck interface to access protected MultilineTextField methods and fields
 */
public interface MultilineTextFieldAccessor {
    boolean bookTweaks$overflowsLineLimit(String string);
    boolean bookTweaks$hasSelection();
    String bookTweaks$truncateInsertionText(String string);
    void bookTweaks$insertText(String string);

    String bookTweaks$getValue();
    int bookTweaks$getCursor();
    int bookTweaks$getSelectCursor();
    void bookTweaks$seekCursor(Whence whence, int i);
    int bookTweaks$getLineAtCursor();

    int bookTweaks$getPreviousWordBeginIndex();
    int bookTweaks$getPreviousWordEndIndex();
}
