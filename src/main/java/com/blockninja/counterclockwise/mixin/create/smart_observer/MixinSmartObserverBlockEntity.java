package com.blockninja.counterclockwise.mixin.create.smart_observer;

import com.blockninja.counterclockwise.CounterClockwiseKt;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.redstone.smartObserver.SmartObserverBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(SmartObserverBlockEntity.class)
public class MixinSmartObserverBlockEntity {
    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", remap = true),
            remap = false
    )
    private static BlockState wrapRelative(Level instance, BlockPos pPos, Operation<BlockState> original, @Local(name = "targetPos") BlockPos targetPos) {
        // We use atomics to get the lambda to be happy but its technically all run in sequence so it's fine
        AtomicReference<BlockState> ogState = new AtomicReference<>(original.call(instance, pPos));
        AtomicReference<BlockPos> newTarget = new AtomicReference<>(targetPos);
        if (ogState.get().isAir()) {
            CounterClockwiseKt.transformToNearbyShipsAndWorldFixed(instance, pPos.getCenter().x, pPos.getCenter().y, pPos.getCenter().z, 0.5, (x, y, z) -> {
                BlockState potentialState = instance.getBlockState(BlockPos.containing(x, y, z));
                if (!potentialState.isAir()) {
                    ogState.set(potentialState);
                    newTarget.set(BlockPos.containing(x, y, z));
                }
            }
            );
        }
        targetPos = newTarget.get();
        return ogState.get();
    }
}
