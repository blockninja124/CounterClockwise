package com.blockninja.counterclockwise.mixin.create.display_link;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.redstone.displayLink.ClickToLinkBlockItem;
import kotlinx.coroutines.BlockingEventLoop;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@Mixin(ClickToLinkBlockItem.class)
public class MixinClickToLinkBlockItem {
    @WrapOperation(
            method = "useOn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;closerThan(Lnet/minecraft/core/Vec3i;D)Z")
    )
    private static boolean wrapDistance(BlockPos instance, Vec3i vec3i, double v, Operation<Boolean> original, @Local(name = "level") Level level) {
        BlockPos newPos = BlockPos.containing(VSGameUtilsKt.toWorldCoordinates(level, instance));
        BlockPos newOtherPos = BlockPos.containing(VSGameUtilsKt.toWorldCoordinates(level, new BlockPos(vec3i)));

        return original.call(newPos, newOtherPos, v);
    }
}
