package com.bedrockscanner;

import com.bedrockscanner.command.ScannerCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class BedrockPatternScannerClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(new ScannerCommand().getRoot());
        });
    }
}