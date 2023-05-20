package com.jorianwoltjer.liveoverflowmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSplitter;

import static com.jorianwoltjer.liveoverflowmod.LiveOverflowMod.LOGGER;
import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.*;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

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

                                    for (BlockPos blockPos : BlockPos.iterate(area.getMinX(), area.getMinY(), area.getMinZ(), area.getMaxX(), area.getMaxY(), area.getMaxZ())) {
                                        String code = "";
                                        if (world.getBlockState(blockPos).getBlock() == Blocks.BEDROCK) {  // Is bedrock
                                            BlockPos pos = blockPos.subtract(start);
                                            code = String.format("generator.is_bedrock(chunk_x + %d, y + %d, chunk_z + %d) == true", pos.getX(), pos.getY(), pos.getZ());
                                        } else if (world.getBlockState(blockPos).getBlock() == Blocks.GLASS) {  // Is empty
                                            BlockPos pos = blockPos.subtract(start);
                                            code = String.format("generator.is_bedrock(chunk_x + %d, y + %d, chunk_z + %d) == false", pos.getX(), pos.getY(), pos.getZ());
                                        } else if (world.getBlockState(blockPos).getBlock() == Blocks.REDSTONE_BLOCK) {  // Is unused bedrock
                                            BlockPos pos = blockPos.subtract(start);
                                            code = String.format("// generator.is_bedrock(chunk_x + %d, y + %d, chunk_z + %d) == true", pos.getX(), pos.getY(), pos.getZ());
                                        } else if (world.getBlockState(blockPos).getBlock() == Blocks.RED_STAINED_GLASS) {  // Is unused empty
                                            BlockPos pos = blockPos.subtract(start);
                                            code = String.format("// generator.is_bedrock(chunk_x + %d, y + %d, chunk_z + %d) == false", pos.getX(), pos.getY(), pos.getZ());
                                        }
                                        if (!code.isEmpty()) {
                                            context.getSource().sendFeedback(Text.of(code), false);
                                        }
                                    }

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
