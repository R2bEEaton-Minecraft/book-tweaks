package cc.spea.booktweaks;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class BookTweaksClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Initialize page memory manager
		PageMemoryManager.init();

		// Save page memory when stopping the client
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			PageMemoryManager.save();
		});
	}
}