package com.bedrockscanner.command;

import com.bedrockscanner.scanner.ScannerUtil;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ScannerCommand {
    private final LiteralArgumentBuilder<FabricClientCommandSource> rootCommand =
            ClientCommandManager.literal("patternScan");
    
    public ScannerCommand() {
        rootCommand.then(ClientCommandManager.argument("number", IntegerArgumentType.integer())
                .executes(ctx -> {
                    int value = IntegerArgumentType.getInteger(ctx, "number");
                    if (value < ScannerUtil.MIN_RADIUS || value > ScannerUtil.MAX_RADIUS) {
                        ctx.getSource().sendFeedback(
                                Text.literal("wrong param. " + 
                                        ScannerUtil.MIN_RADIUS + "~" + 
                                        ScannerUtil.MAX_RADIUS)
                                        .formatted(Formatting.DARK_RED));
                    }
                    ScannerUtil.scanAndSave(value)
                            .thenAccept(V -> {
                                MinecraftClient.getInstance().execute(() -> {
                                    ctx.getSource().sendFeedback(
                                            Text.literal("scan complete")
                                                    .formatted(Formatting.DARK_GREEN));
                                });
                            })
                            .exceptionally(ex -> {
                                MinecraftClient.getInstance().execute(() -> {
                                    ctx.getSource().sendFeedback(
                                            Text.literal("scan failed: " + ex.getMessage())
                                                    .formatted(Formatting.DARK_RED));
                                });
                                return null;
                            });
                    
                    return 1;
                }));
    }
    public LiteralArgumentBuilder<FabricClientCommandSource> getRoot() {
        return rootCommand;
    }
}
