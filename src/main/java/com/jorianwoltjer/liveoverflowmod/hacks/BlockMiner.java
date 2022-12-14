package com.jorianwoltjer.liveoverflowmod.hacks;

import com.jorianwoltjer.liveoverflowmod.mixin.ClientConnectionMixin;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MiningToolItem;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import static com.jorianwoltjer.liveoverflowmod.LiveOverflowMod.PREFIX;
import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.client;
import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.networkHandler;

public class BlockMiner extends ToggledHack {
    BlockPos targetPos;
    int timer;
    public Item itemToPlace;

    int statsValue = -1;
    int statsTimer = 0;

    /**
     * Breaks and places blocks *really* fast.
     * Hold a block in your offhand, and the best tool in your main hand. This hack will automatically fill the offhand with
     * more blocks of that type from your inventory if it runs out.
     * @see ClientConnectionMixin
     */
    public BlockMiner() {
        super("Block Miner", GLFW.GLFW_KEY_BACKSLASH);
    }

    @Override
    public void tickEnabled() {
        if (client.player == null || client.interactionManager == null) return;

        if (++statsTimer > 20*60) {  // 1 minute
            statsTimer = 0;
            networkHandler.sendPacket(new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS));
        }

        if (++timer % 2 != 0) return;  // Only run every few ticks
        if (!(client.player.getMainHandStack().getItem() instanceof MiningToolItem)) return;  // Stop if not holding a tool

        for (int i = 0; i < 9; i++) {
            if (client.player.getOffHandStack().getCount() == 0) {
                int slot = findItemSlot(itemToPlace);
                if (slot == -1) {
                    message("§cNo cobblestone in inventory");
                    return;
                }
                client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, slot, 40, SlotActionType.SWAP, client.player);
            }

            placeOffhandAt(targetPos);
            networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, targetPos, Direction.UP));
            networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, targetPos, Direction.UP));
        }
    }

    @Override
    public void onEnable() {
        if (client.player == null) return;

        timer = 0;
        statsTimer = 0;
        statsValue = -1;
        itemToPlace = client.player.getOffHandStack().getItem();

        if (itemToPlace == Items.AIR) {
            message("§cNo block in offhand");
            enabled = false;
            return;
        }

        networkHandler.sendPacket(new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS));  // Send initial

        switch (client.player.getHorizontalFacing()) {
            case NORTH -> targetPos = client.player.getBlockPos().add(0, 0, -1);
            case SOUTH -> targetPos = client.player.getBlockPos().add(0, 0, 1);
            case WEST -> targetPos = client.player.getBlockPos().add(-1, 0, 0);
            case EAST -> targetPos = client.player.getBlockPos().add(1, 0, 0);
            default -> targetPos = client.player.getBlockPos();
        }
    }

    public void onStatResponse(Integer newValue) {  // Will get a callback from ClientConnectionMixin
        if (!enabled || client.player == null) return;

        if (newValue == null) {  // If not updated, value will be null
            newValue = statsValue;
        }
        if (statsValue == -1) {  // Initial value
            statsValue = newValue;
        }

        client.player.sendMessage(Text.of(String.format(PREFIX + "%s: §b%d§r/min (§a%s§r total)",
                itemToPlace.getName().getString(),
                newValue - statsValue,
                newValue == -1 ? "?" : newValue
        )), false);

        statsValue = newValue;
    }

    public int findItemSlot(Item item) {
        if (client.player == null) return -1;

        for (int i = 9; i < 36; i++) {  // Inventory slots
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                return i;
            }
        }
        return -1;  // Not found
    }

    public static void placeOffhandAt(BlockPos pos) {
        BlockHitResult hitResult = new BlockHitResult(
                new Vec3d(pos.getX(), pos.getY(), pos.getZ()),
                Direction.UP,
                pos,
                false
        );
        networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.OFF_HAND, hitResult, 0));
    }
}
