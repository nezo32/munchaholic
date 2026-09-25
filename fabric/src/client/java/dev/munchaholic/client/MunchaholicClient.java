package dev.munchaholic.client;

import net.fabricmc.api.ClientModInitializer;

/** Client entrypoint: notification settings, the roll payload receiver, the notify command and the recipe tooltip. */
public final class MunchaholicClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		NotifyConfig.load();
		NotifyClient.register();
		NotifyCommand.register();
		ClientRecipeBook.register();
		RecipeTooltip.register();
	}
}
