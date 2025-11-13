package cc.spea.booktweaks.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration for Book Tweaks mod.
 */
public class BookTweaksConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("book-tweaks");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("config.json");

    public int maxBookMemoryEntries = 100;

    /**
     * Load configuration from file, or create default if not exists.
     */
    public static BookTweaksConfig load() {
        // Ensure config directory exists
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            System.err.println("Failed to create book-tweaks config directory: " + e.getMessage());
        }

        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                return GSON.fromJson(json, BookTweaksConfig.class);
            } catch (IOException e) {
                System.err.println("Failed to load book-tweaks config, using defaults: " + e.getMessage());
            }
        }

        // Create default config
        BookTweaksConfig config = new BookTweaksConfig();
        config.save();
        return config;
    }

    /**
     * Save configuration to file.
     */
    public void save() {
        try {
            // Ensure directory exists
            Files.createDirectories(CONFIG_DIR);
            String json = GSON.toJson(this);
            Files.writeString(CONFIG_PATH, json);
        } catch (IOException e) {
            System.err.println("Failed to save book-tweaks config: " + e.getMessage());
        }
    }
}
