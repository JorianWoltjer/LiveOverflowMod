package com.jorianwoltjer.liveoverflowmod.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.jorianwoltjer.liveoverflowmod.client.ClientEntrypoint.*;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    // Insta-Mine
    @Inject(method = "attackBlock", at = @At(value = "HEAD"), cancellable = true)
    private void attackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (client.world == null || client.player == null) return;

        if (passiveMods.enabled) {
            BlockState blockState = client.world.getBlockState(pos);
            double speed = blockState.calcBlockBreakingDelta(client.player, client.world, pos);
            if (!blockState.isAir() && speed > 0.5F) {  // If you can break the block fast enough, break it instantly
                client.world.breakBlock(pos, true, client.player);
                networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, direction));
                networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, direction));
                cir.setReturnValue(true);  // Return true to break the block on the client-side
            }
        }
    }

}
