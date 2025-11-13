package cc.spea.booktweaks;

import cc.spea.booktweaks.config.BookTweaksConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages memory of which page each book was on when last closed.
 * Uses LRU cache with persistent storage.
 */
public class PageMemoryManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("book-tweaks");
    private static final Path DATA_PATH = CONFIG_DIR.resolve("page-memory.json");
    private static BookTweaksConfig config;
    private static Map<String, Integer> bookPages;

    /**
     * Initialize the page memory manager.
     * Loads config and restores saved data.
     */
    public static void init() {
        config = BookTweaksConfig.load();
        bookPages = createLRUMap(config.maxBookMemoryEntries);
        load();
    }

    /**
     * Create an LRU (Least Recently Used) map with the given maximum size.
     */
    private static Map<String, Integer> createLRUMap(int maxSize) {
        return new LinkedHashMap<String, Integer>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
                return size() > maxSize;
            }
        };
    }

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
            // Create a stable hash based on the actual page content
            // This ensures the same content always produces the same ID
            int contentHash = 0;
            for (var page : writableBook.pages().stream().toList()) {
                // Combine hashes using a stable algorithm (page.raw() gets the unfiltered text)
                contentHash = 31 * contentHash + (page != null ? page.raw().hashCode() : 0);
            }
            id.append(contentHash);
            return id.toString();
        }

        return null;
    }

    /**
     * Remember which page a book was on.
     */
    public static void rememberPage(ItemStack book, int page) {
        if (bookPages == null) {
            init();
        }

        String bookId = getBookId(book);
        if (bookId != null) {
            bookPages.put(bookId, page);
            // Auto-save after each update for better persistence
            save();
        }
    }

    /**
     * Get the remembered page for a book, or null if not remembered.
     */
    public static Integer getRememberedPage(ItemStack book) {
        if (bookPages == null) {
            init();
        }

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
        if (bookPages == null) {
            return;
        }

        String bookId = getBookId(book);
        if (bookId != null) {
            bookPages.remove(bookId);
            save();
        }
    }

    /**
     * Clear all remembered pages.
     */
    public static void clearAll() {
        if (bookPages != null) {
            bookPages.clear();
            save();
        }
    }

    /**
     * Save the page memory to disk.
     */
    public static void save() {
        if (bookPages == null) {
            return;
        }

        try {
            // Ensure directory exists
            Files.createDirectories(CONFIG_DIR);
            String json = GSON.toJson(bookPages);
            Files.writeString(DATA_PATH, json);
        } catch (IOException e) {
            System.err.println("Failed to save book page memory: " + e.getMessage());
        }
    }

    /**
     * Load the page memory from disk.
     */
    private static void load() {
        if (!Files.exists(DATA_PATH)) {
            return;
        }

        try {
            String json = Files.readString(DATA_PATH);
            Type type = new TypeToken<LinkedHashMap<String, Integer>>(){}.getType();
            Map<String, Integer> loaded = GSON.fromJson(json, type);

            if (loaded != null) {
                // Load entries into our LRU map, respecting the current size limit
                for (Map.Entry<String, Integer> entry : loaded.entrySet()) {
                    bookPages.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load book page memory: " + e.getMessage());
        }
    }
}
