package com.jorianwoltjer.liveoverflowmod.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import static com.jorianwoltjer.liveoverflowmod.client.Keybinds.packetQueue;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;
import static com.jorianwoltjer.liveoverflowmod.LiveOverflowMod.LOGGER;

public class ClipCommand {
    private static void clipVehicleTo(Entity vehicle, Vec3d pos) {
        packetQueue.add(new VehicleMoveC2SPacket(vehicle));  // Queue packet
//        networkHandler.sendPacket(new VehicleMoveC2SPacket(vehicle));  // First packet (original position)
        vehicle.updatePosition(pos.x, pos.y, pos.z);
        packetQueue.add(new VehicleMoveC2SPacket(vehicle));  // Queue packet
//        networkHandler.sendPacket(new VehicleMoveC2SPacket(vehicle));  // Far packet
    }

    private static void moveVehicleTo(Entity vehicle, Vec3d pos) {
        vehicle.updatePosition(pos.x, pos.y, pos.z);
        packetQueue.add(new VehicleMoveC2SPacket(vehicle));  // Queue packet
//        networkHandler.sendPacket(new VehicleMoveC2SPacket(vehicle));  // Far packet
    }

    private static void pressButtonAt(PlayerEntity player, BlockPos pos) {
        packetQueue.add(
                new PlayerInteractBlockC2SPacket(
                        player.preferredHand,
                        new BlockHitResult(
                                new Vec3d(pos.getX(), pos.getY(), pos.getZ()),
                                Direction.DOWN,
                                pos,
                                false
                        ),
                        0
                )
        );
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("vault")
            .executes(context -> {
                PlayerEntity player = context.getSource().getPlayer();
                assert player != null;
                Entity vehicle = player.getVehicle();
                assert vehicle != null;

                LOGGER.info("Pressing start button");
                pressButtonAt(player, new BlockPos(4729, 125, 1337));  // Start button

                // y += 53.0
                LOGGER.info("Moving to ceiling right above end button");
                moveVehicleTo(vehicle, vehicle.getPos().add(0, 9.9, 0));
                moveVehicleTo(vehicle, vehicle.getPos().add(0, 9.9, 0));
                moveVehicleTo(vehicle, vehicle.getPos().add(0, 9.9, 0));
                moveVehicleTo(vehicle, vehicle.getPos().add(0, 9.9, 0));
                moveVehicleTo(vehicle, vehicle.getPos().add(0, 9.9, 0));
                moveVehicleTo(vehicle, vehicle.getPos().add(0, 3.5, 0));

                // x += 53.0
                moveVehicleTo(vehicle, vehicle.getPos().add(9.9, 0, 0));
                moveVehicleTo(vehicle, vehicle.getPos().add(9.9, 0, 0));
                moveVehicleTo(vehicle, vehicle.getPos().add(9.9, 0, 0));
                moveVehicleTo(vehicle, vehicle.getPos().add(9.9, 0, 0));
                moveVehicleTo(vehicle, vehicle.getPos().add(9.9, 0, 0));
                moveVehicleTo(vehicle, vehicle.getPos().add(3.0, 0, 0));

                // y -= 53.0 (through box)
                LOGGER.info("Clipping down through box");
                clipVehicleTo(vehicle, vehicle.getPos().add(0, -53, 0));

                LOGGER.info("Pressing end button");
                pressButtonAt(player, new BlockPos(4780, 125, 1336));  // End button

                return 1;
            })
        );

        dispatcher.register(literal("vclip")
            .then(argument("distance", integer())
                .executes(context -> {
                    int distance = context.getArgument("distance", Integer.class);

                    PlayerEntity player = context.getSource().getPlayer();
                    assert player != null;
                    Entity vehicle = player.getVehicle();
                    assert vehicle != null;
                    clipVehicleTo(vehicle, vehicle.getPos().add(0, distance, 0));

                    return 1;
                })
            ));

        // Clip into viewing direction
        dispatcher.register(literal("hclip")
            .then(argument("distance", integer())
                .executes(context -> {
                    int distance = context.getArgument("distance", Integer.class);

                    PlayerEntity player = context.getSource().getPlayer();
                    assert player != null;
                    Entity vehicle = player.getVehicle();
                    assert vehicle != null;

                    double yaw = floatMod(player.getYaw(), 360);

                    if (yaw < 270+45 && yaw > 270-45) {
                        clipVehicleTo(vehicle, vehicle.getPos().add(distance, 0, 0));
                    } else if (yaw < 180+45 && yaw > 180-45) {
                        clipVehicleTo(vehicle, vehicle.getPos().add(0, 0, -distance));
                    } else if (yaw < 90+45 && yaw > 90-45) {
                        clipVehicleTo(vehicle, vehicle.getPos().add(-distance, 0, 0));
                    } else {
                        clipVehicleTo(vehicle, vehicle.getPos().add(0, 0, distance));
                    }

                    return 1;
                })
            ));
    }

    private static double floatMod(double x, double y) {
        // x mod y behaving the same way as Math.floorMod but with doubles
        return (x - Math.floor(x/y) * y);
    }
}
