package com.jorianwoltjer.liveoverflowmod.mixin;

import com.jorianwoltjer.liveoverflowmod.client.Keybinds;
import net.minecraft.block.AbstractBlock;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Objects;
import java.util.Random;

@Mixin(AbstractBlock.class)
public class AbstractBlockMixin {
    private static final long xFactor = new Random().nextLong();
    private static final long yFactor = new Random().nextLong();
    private static final long zFactor = new Random().nextLong();

    // Randomize texture rotations (prevent leaks)
    @Redirect(method="getRenderingSeed", at=@At(value="INVOKE", target="Lnet/minecraft/util/math/MathHelper;hashCode(Lnet/minecraft/util/math/Vec3i;)J"))
    private long getSeed(Vec3i vec) {
        if (Keybinds.passiveModsEnabled) {
            return Objects.hash(vec.getX() * xFactor, vec.getY() * yFactor, vec.getZ() * zFactor);
        } else {
            return MathHelper.hashCode(vec);
        }
    }
}
