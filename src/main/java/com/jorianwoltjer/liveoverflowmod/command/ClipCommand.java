package com.jorianwoltjer.liveoverflowmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.*;
import static com.jorianwoltjer.liveoverflowmod.LiveOverflowMod.LOGGER;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class ClipCommand {
    private static void interactAt(BlockPos pos) {
        if (client.interactionManager == null) return;

        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, new BlockHitResult(
                new Vec3d(pos.getX(), pos.getY(), pos.getZ()),
                Direction.DOWN,
                pos,
                false
        ));
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("vault")
                .executes(context -> {
                    PlayerEntity player = context.getSource().getPlayer();
                    Vec3d pos = player.getPos();

                    interactAt(new BlockPos(4729, 125, 1337));  // Start button

                    // y += 53.0
                    for (int i = 0; i < 5; i++) {
                        pos = pos.add(0, 9.9, 0);
                        networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true));
                    }
                    pos = pos.add(0, 3.5, 0);
                    networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true));

                    // x += 53.0
                    for (int i = 0; i < 5; i++) {
                        pos = pos.add(9.9, 0, 0);
                        networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true));
                    }
                    pos = pos.add(3.0, 0, 0);
                    networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true));

                    // y -= 53.0 (through box)
                    pos = pos.add(0, -53, 0);
                    networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true));
                    player.setPosition(pos);

                    interactAt(new BlockPos(4780, 125, 1336));  // End button

                    return 1;
                })
        );

        dispatcher.register(literal("vclip")  // Vertical clip
            .then(argument("distance", integer())
                .executes(context -> {
                    int distance = context.getArgument("distance", Integer.class);
                    PlayerEntity player = context.getSource().getPlayer();

                    Vec3d pos = player.getPos();
                    Vec3d targetPos = pos.add(0, distance, 0);

                    clipStraight(targetPos);

                    return 1;
                })
            )
        );

        dispatcher.register(literal("hclip")  // Horizontal clip (up -> horizontal -> down: to go through walls)
                .then(argument("distance", integer())
                        .executes(context -> {
                            int distance = context.getArgument("distance", Integer.class);

                            assert client.player != null;

                            // Move `direction` blocks into viewing direction
                            Vec3d targetPos = client.player.getPos().add(
                                    client.player.getRotationVector().multiply(1, 0, 1).normalize().multiply(distance)
                            );
                            clipUpDown(targetPos);

                            return 1;
                        })
                )
        );

        dispatcher.register(literal("dclip")  // Directional clip
            .then(argument("distance", integer())
                .executes(context -> {
                    int distance = context.getArgument("distance", Integer.class);
                    PlayerEntity player = context.getSource().getPlayer();

                    Vec3d pos = player.getPos();
                    // Move into players viewing direction
                    Vec3d targetPos = pos.add(player.getRotationVector().normalize().multiply(distance));

                    clipStraight(targetPos);

                    return 1;
                })
            )
        );

        dispatcher.register(literal("autoclip")
            .then(literal("up")
                .executes(context -> executeAutoClip(context, 1))
            )
            .then(literal("down")
                .executes(context -> executeAutoClip(context, -1))
            )
        );

        dispatcher.register(literal("clubmate")
            .executes(context -> {
                clipStraight(new Vec3d(1331, 89, 1330));  // Next to chest

                assert client.player != null;
                interactAt(new BlockPos(1331, 89, 1331));  // Chest

                return 1;
            })
        );
    }

    private static void clipStraight(Vec3d targetPos) {
        if (client.player == null) return;

        Vec3d pos = client.player.getPos();

        for (int i = 0; i < 18; i++) {  // Send a lot of the same movement packets to increase max travel distance
            moveTo(pos);
        }
        // Send one big movement packet to actually move the player
        moveTo(targetPos);
    }

    private static void clipUpDown(Vec3d targetPos) {
        if (client.player == null) return;

        Vec3d pos = client.player.getPos();

        for (int i = 0; i < 15; i++) {  // Send a lot of the same movement packets to increase max travel distance
            moveTo(pos);
        }

        pos = pos.add(0, 100, 0);  // Up
        moveTo(pos);

        pos = new Vec3d(targetPos.x, pos.y, targetPos.z);  // Horizontal
        moveTo(pos);

        moveTo(targetPos);  // Down
    }

    private static int executeAutoClip(CommandContext<FabricClientCommandSource> context, int direction) {
        if (client.player == null) return 0;

        Vec3d pos = getAutoClipPos(direction);
        if (pos == null) {
            context.getSource().sendFeedback(Text.of("§cNo valid position found within 150 blocks"));
            return 0;
        } else {
            context.getSource().sendFeedback(Text.of(String.format("§7Clipping §a%.0f§7 blocks", pos.y - (int) client.player.getPos().y)));
            clipStraight(pos);
            return 1;
        }
    }

    /**
     * Automatically go through the nearest blocks in the given direction.
     * Credits to @EnderKill98 for the original code.
     */
    private static Vec3d getAutoClipPos(int direction) {
        if (client.player == null || client.world == null) return null;

        boolean inside = false;
        for (float i = 0; i < 150; i += 0.25) {
            Vec3d pos = client.player.getPos();
            Vec3d targetPos = pos.add(0, direction * i, 0);

            boolean collides = !client.world.isSpaceEmpty(client.player, client.player.getBoundingBox().offset(targetPos.subtract(pos)));

            if (!inside && collides) {  // Step 1: Into the blocks
                inside = true;
            } else if (inside && !collides) {  // Step 2: Out of the blocks
                return targetPos;
            }
        }

        return null;  // Nothing found
    }

    private static void moveTo(Vec3d pos) {
        if (client.player == null) return;

        if (client.player.getVehicle() != null) {
            client.player.getVehicle().setPosition(pos);
            networkHandler.sendPacket(new VehicleMoveC2SPacket(client.player.getVehicle()));
        } else {
            client.player.setPosition(pos);
            networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true));
        }
    }
}
