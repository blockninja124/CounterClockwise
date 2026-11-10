package com.blockninja.counterclockwise.mixin.create.processing;

import com.blockninja.counterclockwise.CounterClockwiseKt;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ConcurrentModificationException;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(BlockEntityBehaviour.class)
public class MixinBlockEntityBehaviour {
    @Shadow
    public static <T extends BlockEntityBehaviour> T get(BlockEntity be, BehaviourType<T> type) {
        return null;
    }

    @WrapOperation(
            method = "get(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lcom/simibubi/create/foundation/blockEntity/behaviour/BehaviourType;)Lcom/simibubi/create/foundation/blockEntity/behaviour/BlockEntityBehaviour;",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/blockEntity/behaviour/BlockEntityBehaviour;get(Lnet/minecraft/world/level/block/entity/BlockEntity;Lcom/simibubi/create/foundation/blockEntity/behaviour/BehaviourType;)Lcom/simibubi/create/foundation/blockEntity/behaviour/BlockEntityBehaviour;", remap = true),
            remap = false
    )
    private static <T extends BlockEntityBehaviour> T wrapGet(BlockEntity be, BehaviourType<T> type, Operation<T> original, @Local(argsOnly = true) BlockGetter getter, @Local(argsOnly = true) BlockPos pos) {
        T ogReturn = original.call(be, type);
        if (type != BeltProcessingBehaviour.TYPE) return ogReturn;
        if (ogReturn != null) return ogReturn;

        AtomicReference<BlockEntity> newBe = new AtomicReference<>(null);
        CounterClockwiseKt.transformToNearbyShipsAndWorldFixed((Level) getter, pos.getCenter().x, pos.getCenter().y, pos.getCenter().z, 0.5, (x, y, z) -> {
            BlockEntity tempBE;
            try {
                tempBE = getter.getBlockEntity(BlockPos.containing(x, y, z));
            } catch (ConcurrentModificationException e) {
                tempBE = null;
            }
            if (tempBE != null) {
                newBe.set(tempBE);
            }
        });


        return get(newBe.get(), type);
    }
}
