package com.bedrockscanner.scanner;

import com.bedrockscanner.BedrockPatternScanner;
import com.bedrockscanner.scanner.exception.InvalidWorldObjectException;
import com.bedrockscanner.scanner.exception.WrongParamException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class ScannerUtil {
    private static final Logger LOGGER = BedrockPatternScanner.LOGGER;
    
    public static final int MAX_RADIUS = 1;
    public static final int MIN_RADIUS = 0;
    private static final Path SAVE_PATH = FabricLoader.getInstance()
            .getGameDir()
            .resolve(BedrockPatternScanner.MOD_ID);
    
    private ScannerUtil() {}
    
    public static CompletableFuture<Void> scanAndSave(int radius) {
        if (radius > MAX_RADIUS || radius < MIN_RADIUS) {
            LOGGER.warn("wrong parameter: {}", radius);
            return CompletableFuture.failedFuture(new WrongParamException("wrong radius: " + radius));
        }
        try {
            Files.createDirectories(SAVE_PATH);
            ClientWorld world = MinecraftClient.getInstance().world;
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (world == null || player == null) {
                LOGGER.warn("null client object: world || player");
                return CompletableFuture.failedFuture(new InvalidWorldObjectException("null client object: world || player"));
            }
            if (world.getDimension().cardinalLightType() != DimensionType.CardinalLightType.NETHER) {
                LOGGER.warn("wrong dimension");
                return CompletableFuture.failedFuture(new InvalidWorldObjectException("wrong dimension"));
            }
            
            int size = 2*radius + 1;
            ChunkPos centerPos = player.getChunkPos();
            List<Long> bedrockPosList = new ArrayList<>();

            BlockPos.Mutable mutable = new BlockPos.Mutable();
            
            return CompletableFuture.supplyAsync(() -> {
                try {
                    for (int i = 0; i < size * size; i++) {
                        int chunkX = centerPos.x - radius + i % size;
                        int chunkZ = centerPos.z - radius + i / size;

                        Chunk chunk = world.getChunk(chunkX, chunkZ);

                        int startX = chunkX << 4;
                        int startZ = chunkZ << 4;

                        for (int j = 0; j < 16; j++) {
                            for (int k = 0; k < 16; k++) {
                                if (chunk.getBlockState(mutable.set(startX + j, 3, startZ + k))
                                        .isOf(Blocks.BEDROCK))
                                    bedrockPosList.add(BlockPos.asLong(startX + j, 3, startZ + k));
                                if (chunk.getBlockState(mutable.set(startX + j, 123, startZ + k))
                                        .isOf(Blocks.BEDROCK))
                                    bedrockPosList.add(BlockPos.asLong(startX + j, 123, startZ + k));
                            }
                        }
                    }
                    
                    String fileName = String.format("%d, %d %d.txt", centerPos.x, centerPos.z, radius);
                    Path filePath = SAVE_PATH.resolve(fileName);
                    Files.writeString(filePath, "",
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE);

                    for (long pos : bedrockPosList) {
                        String str = String.format(
                                "%d %d %d bedrock\n",
                                BlockPos.unpackLongX(pos), BlockPos.unpackLongY(pos), BlockPos.unpackLongZ(pos));

                        Files.writeString(filePath, str, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    }
                    
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }, Util.getIoWorkerExecutor());
        } catch (IOException e) {
            LOGGER.warn("scan failed! detail: {}", e.toString());
            return CompletableFuture.failedFuture(new IOException("IOException occurred", e));
        }
    }
}
