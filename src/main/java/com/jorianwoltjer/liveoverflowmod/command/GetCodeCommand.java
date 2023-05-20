package com.jorianwoltjer.liveoverflowmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSplitter;

import static com.jorianwoltjer.liveoverflowmod.LiveOverflowMod.LOGGER;
import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.*;

import static net.minecraft.client.util.SelectionManager.setClipboard;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class GetCodeCommand {
    private static final Dynamic2CommandExceptionType TOO_BIG_EXCEPTION = new Dynamic2CommandExceptionType((maxCount, count) -> Text.translatable("commands.fill.toobig", maxCount, count));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Checks to see if bedrock is at a certain position (for tracing in debugger)
        dispatcher.register(literal("bedrock")
                .then(argument("pos", BlockPosArgumentType.blockPos())
                        .executes(context -> {
                            BlockPos pos = BlockPosArgumentType.getBlockPos(context, "pos");
                            ServerWorld world = context.getSource().getWorld();

                            boolean result = checkBedrock(world, pos);  // <--- Try setting a breakpoint here

                            context.getSource().sendFeedback(Text.of("Bedrock at " + pos.getX() + " " + pos.getY() + " " + pos.getZ() +
                                    ": " + (result ? "§atrue" : "§cfalse")), false);

                            return 1;
                        })
                )
        );

        // Generate the checks for the Rust code to brute-force a recreated bedrock formation
        dispatcher.register(literal("getcode")
                .then(argument("from", BlockPosArgumentType.blockPos())
                        .then(argument("to", BlockPosArgumentType.blockPos())
                                .executes(context -> {
                                    BlockBox area = BlockBox.create(BlockPosArgumentType.getLoadedBlockPos(context, "from"), BlockPosArgumentType.getLoadedBlockPos(context, "to"));
                                    ChunkPos centerChunk = new ChunkPos(area.getCenter());
                                    BlockPos start = new BlockPos(centerChunk.getStartX(), area.getMaxY(), centerChunk.getStartZ());

                                    int i = area.getBlockCountX() * area.getBlockCountY() * area.getBlockCountZ();
                                    if (i > 32768) {
                                        throw TOO_BIG_EXCEPTION.create(32768, i);
                                    }

                                    ServerWorld world = context.getSource().getWorld();
                                    StringBuilder code = new StringBuilder();

                                    for (BlockPos blockPos : BlockPos.iterate(area.getMinX(), area.getMinY(), area.getMinZ(), area.getMaxX(), area.getMaxY(), area.getMaxZ())) {
                                        Block block = world.getBlockState(blockPos).getBlock();
                                        if (block == Blocks.BEDROCK) {
                                            BlockPos pos = blockPos.subtract(start);
                                            code.append(String.format(" generator.is_bedrock(chunk_x + %d, y + %d, chunk_z + %d) &&\n", pos.getX(), pos.getY(), pos.getZ()));
                                        } else if (block == Blocks.GLASS) {
                                            BlockPos pos = blockPos.subtract(start);
                                            code.append(String.format("!generator.is_bedrock(chunk_x + %d, y + %d, chunk_z + %d) &&\n", pos.getX(), pos.getY(), pos.getZ()));
                                        } else if (block == Blocks.REDSTONE_BLOCK) {
                                            BlockPos pos = blockPos.subtract(start);
                                            code.append(String.format("//  generator.is_bedrock(chunk_x + %d, y + %d, chunk_z + %d) &&\n", pos.getX(), pos.getY(), pos.getZ()));
                                        } else if (block == Blocks.RED_STAINED_GLASS) {
                                            BlockPos pos = blockPos.subtract(start);
                                            code.append(String.format("// !generator.is_bedrock(chunk_x + %d, y + %d, chunk_z + %d) &&\n", pos.getX(), pos.getY(), pos.getZ()));
                                        }
                                    }

                                    setClipboard(client, code.toString());

                                    int lineCount = code.toString().split("\n").length;
                                    context.getSource().sendFeedback(Text.literal("Copied ")
                                            .append(Text.literal(String.valueOf(lineCount)).formatted(Formatting.GREEN))
                                            .append(Text.literal(" lines to clipboard")), false);

                                    return 1;
                                })
                        )
                )
        );
    }

    private static boolean checkBedrock(ServerWorld world, BlockPos pos) {
        RandomSplitter randomSplitter = world.getChunkManager().getNoiseConfig().getOrCreateRandomDeriver(new Identifier("bedrock_floor"));

        int i = 0;
        int j = 5;

        int i2 = pos.getY();
        if (i2 <= i) {
            return true;
        }
        if (i2 >= j) {
            return false;
        }
        double d = MathHelper.map(i2, i, j, 1.0, 0.0);
        LOGGER.info("d: " + d);

        Random random = randomSplitter.split(pos.getX(), i, pos.getZ());

        return (double)random.nextFloat() < d;
    }
}
