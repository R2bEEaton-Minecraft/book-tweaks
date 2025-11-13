package cc.spea.booktweaks;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages memory of which page each book was on when last closed.
 */
public class PageMemoryManager {
    private static final Map<String, Integer> bookPages = new HashMap<>();

    /**
     * Get a unique identifier for a book ItemStack.
     * Uses DataComponents to distinguish between different books.
     */
    private static String getBookId(ItemStack book) {
        if (book == null || book.isEmpty()) {
            return null;
        }

        // Create a unique ID based on the book's content
        StringBuilder id = new StringBuilder();

        // Check for written book first
        WrittenBookContent writtenBook = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (writtenBook != null) {
            id.append(writtenBook.title().raw()).append(":");
            id.append(writtenBook.author()).append(":");
            id.append(writtenBook.hashCode());
            return id.toString();
        }

        // Check for writable book
        WritableBookContent writableBook = book.get(DataComponents.WRITABLE_BOOK_CONTENT);
        if (writableBook != null) {
            id.append(writableBook.hashCode());
            return id.toString();
        }

        return null;
    }

    /**
     * Remember which page a book was on.
     */
    public static void rememberPage(ItemStack book, int page) {
        String bookId = getBookId(book);
        if (bookId != null) {
            bookPages.put(bookId, page);
        }
    }

    /**
     * Get the remembered page for a book, or null if not remembered.
     */
    public static Integer getRememberedPage(ItemStack book) {
        String bookId = getBookId(book);
        if (bookId != null) {
            return bookPages.get(bookId);
        }
        return null;
    }

    /**
     * Clear memory for a specific book.
     */
    public static void forget(ItemStack book) {
        String bookId = getBookId(book);
        if (bookId != null) {
            bookPages.remove(bookId);
        }
    }

    /**
     * Clear all remembered pages.
     */
    public static void clearAll() {
        bookPages.clear();
    }
}
