package cc.spea.booktweaks.accessor;

/**
 * Duck interface to access protected MultilineTextField methods and fields
 */
public interface MultilineTextFieldAccessor {
    boolean bookTweaks$overflowsLineLimit(String string);
    boolean bookTweaks$hasSelection();
    String bookTweaks$truncateInsertionText(String string);

    String bookTweaks$getValue();
    int bookTweaks$getCursor();
    int bookTweaks$getSelectCursor();
}
