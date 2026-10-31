package com.blockninja.counterclockwise.mixin.create.sticker;

import com.blockninja.counterclockwise.create.sticker.StickerJointManager;
import com.simibubi.create.content.contraptions.chassis.StickerBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

@Pseudo
@Mixin(StickerBlockEntity.class)
public abstract class MixinStickerBlockEntity extends SmartBlockEntity {

    @Unique
    private boolean needUpdate = false;

    @Unique
    private boolean wasBlockStateExtended = false;

    @Unique
    private StickerJointManager manager = null;

    public MixinStickerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/foundation/blockEntity/SmartBlockEntity;tick()V",
                    shift = At.Shift.AFTER
            ),
            remap = false
    )
    public void stickerConstraints(CallbackInfo ci) {
        if (this.level == null || this.level.isClientSide()) return;

        boolean isExtended = this.isBlockStateExtended();
        StickerJointManager manager = this.getManager();

        if (manager != null) {
            if (isExtended) {
                manager.checkStickerJoint();
            }

            if (isExtended != this.wasBlockStateExtended) {
                this.needUpdate = true;
                this.wasBlockStateExtended = isExtended;
            }

            if (this.needUpdate) {
                if (isExtended) {
                    manager.createStickerJoint();
                } else {
                    manager.removeAllJointGroups();
                }
                this.needUpdate = false;
            }
        }
    }

    @Inject(
            method = "write",
            at = @At("TAIL"),
            remap = false
    )
    private void write(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        StickerJointManager manager = getManager();
        if (manager != null) {
            manager.writeCompoundTag(tag);
        }
    }

    @Inject(
            method = "read",
            at = @At("TAIL"),
            remap = false
    )
    private void read(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        StickerJointManager manager = getManager();
        if (manager != null) {
            manager.readCompoundTag(tag);
        }
    }

    @Override
    public void remove() {
        final StickerJointManager manager = getManager();
        if (manager != null) {
            manager.removeAllJointGroups();
        }
        super.remove();
    }

    @Unique
    private StickerJointManager getManager() {
        if (this.level == null || this.level.isClientSide) return null;
        if (this.manager == null) {
            final ServerLevel serverLevel = (ServerLevel) this.level;
            this.manager = new StickerJointManager(serverLevel, VSGameUtilsKt.getShipManagingPos(serverLevel, this.worldPosition), worldPosition, this::getFacing, VSGameUtilsKt.getShipObjectWorld(serverLevel));
        }
        return this.manager;
    }

    @Unique
    private Direction getFacing() {
        return this.getBlockState().getValue(FACING);
    }

    @Unique
    public boolean isAirOrFluid(BlockState state) {
        return state.isAir() || state.getFluidState() != Fluids.EMPTY.defaultFluidState();
    }

    @Shadow(remap = false)
    public abstract boolean isBlockStateExtended();
}

