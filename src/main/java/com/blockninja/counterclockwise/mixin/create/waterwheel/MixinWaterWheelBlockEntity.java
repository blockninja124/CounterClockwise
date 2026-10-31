package com.blockninja.counterclockwise.mixin.create.waterwheel;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.waterwheel.WaterWheelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WaterWheelBlockEntity.class)
public abstract class MixinWaterWheelBlockEntity extends GeneratingKineticBlockEntity {

    @Unique
    public Integer jointId = null;

    public MixinWaterWheelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(
            method = "<init>",
            at = @At("RETURN"),
            remap = false
    )
    private void injectInit(BlockEntityType<?> type, BlockPos pos, BlockState state, CallbackInfo ci) {

    }

    @Inject(
            method = "lazyTick",
            at = @At("HEAD"),
            remap = false
    )
    private void injectTick(CallbackInfo ci) {

    }


}
