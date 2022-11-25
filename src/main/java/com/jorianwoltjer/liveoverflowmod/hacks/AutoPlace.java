package com.jorianwoltjer.liveoverflowmod.hacks;

import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.client;
import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.networkHandler;

public class AutoPlace extends ToggledHack {
    /**
     * Automatically place blocks around you.
     * Uses the item in your *offhand* and makes sure to wait if you run out of blocks.
     */
    public AutoPlace() {
        super("Auto Place", GLFW.GLFW_KEY_RIGHT_BRACKET);
    }

    @Override
    public void tickEnabled() {
        if (client.player == null || client.world == null) return;

        BlockPos[] positions = {  // All positions around you (except floor)
                client.player.getBlockPos().add(0, 2, 0),
                client.player.getBlockPos().add(1, 1, 0),
                client.player.getBlockPos().add(0, 1, 1),
                client.player.getBlockPos().add(-1, 1, 0),
                client.player.getBlockPos().add(0, 1, -1),
                client.player.getBlockPos().add(1, 0, 0),
                client.player.getBlockPos().add(0, 0, 1),
                client.player.getBlockPos().add(-1, 0, 0),
                client.player.getBlockPos().add(0, 0, -1),
        };
        // Enough blocks in offhand
        if (client.player.getOffHandStack().getCount() > positions.length) {
            for (BlockPos pos : positions) {
                if (client.world.getBlockState(pos).isAir()) {  // Only place if empty
                    placeAt(pos);
                }
            }
        }
    }

    public static void placeAt(BlockPos pos) {
        BlockHitResult hitResult = new BlockHitResult(
                new Vec3d(pos.getX(), pos.getY(), pos.getZ()),
                Direction.UP,
                pos,
                false
        );
        networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.OFF_HAND, hitResult, 0));
    }
}
