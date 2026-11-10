package com.blockninja.counterclockwise.mixin.create.psi;

import com.blockninja.counterclockwise.mixinducks.create.portable_interface.IPSIWithShipBehavior;
import com.blockninja.counterclockwise.create.psi.PSIWithShip;
import com.blockninja.counterclockwise.create.psi.PortableFluidInterfaceWithShipController;
import com.blockninja.counterclockwise.create.psi.PortableItemInterfaceWithShipController;
import com.google.common.cache.RemovalListener;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.contraptions.DirectionalExtenderScrollOptionSlot;
import com.simibubi.create.content.contraptions.actors.psi.PortableFluidInterfaceBlockEntity;
import com.simibubi.create.content.contraptions.actors.psi.PortableItemInterfaceBlockEntity;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlock;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PortableStorageInterfaceBlockEntity.class)
public abstract class MixinPortableStorageInterfaceBlockEntity implements IPSIWithShipBehavior {

    @Unique
    private PSIWithShip vs_addition$controller = null;

    @Inject(
            method = "initialize",
            at = @At("TAIL"),
            remap = false
    )
    private void onInitialize(CallbackInfo ci) {
        if ((PortableStorageInterfaceBlockEntity)(Object)this instanceof PortableItemInterfaceBlockEntity entity) {
            this.vs_addition$controller = new PortableItemInterfaceWithShipController(entity);
        } else if((PortableStorageInterfaceBlockEntity)(Object)this instanceof PortableFluidInterfaceBlockEntity entity) {
            this.vs_addition$controller = new PortableFluidInterfaceWithShipController(entity);
        }
    }

    @Override
    public void setController(PSIWithShip controller) {
        this.vs_addition$controller = controller;
    }

    @Override
    public PSIWithShip getController() {
        return this.vs_addition$controller;
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/foundation/blockEntity/SmartBlockEntity;tick()V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true,
            remap = false
    )
    private void onTick(CallbackInfo ci) {
        final PSIWithShip controller = this.getController();
        if (controller == null) { return; }
        controller.tick(ci);
    }

    @WrapMethod(
            method = "getExtensionDistance",
            remap = false
    )
    private float replace(float partialTicks, Operation<Float> original) {
        if (this.getWorkingMode().get() == WorkingMode.WITH_SHIP) {
            final PSIWithShip controller = this.getController();
            if (controller != null) {
                return controller.getExtensionDistance(partialTicks);
            }
        }
        return original.call(partialTicks);
    }

    @Unique
    private ScrollOptionBehaviour<WorkingMode> vs_addition$workingMode;

    @Inject(
            method = "addBehaviours",
            at = @At("RETURN"),
            remap = false
    )
    public void behaviour(List<BlockEntityBehaviour> behaviours, CallbackInfo ci) {
        this.vs_addition$workingMode = new ScrollOptionBehaviour<>(WorkingMode.class, CreateLang.translateDirect("vs_addition.working_mode"), (PortableStorageInterfaceBlockEntity)(Object) this, vs_addition$getMovementModeSlot());
        behaviours.add(this.vs_addition$workingMode);
    }

    @Unique
    private ValueBoxTransform vs_addition$getMovementModeSlot() {
        return new DirectionalExtenderScrollOptionSlot((state, d) -> {
            Direction.Axis axis = d.getAxis();
            Direction.Axis bearingAxis = state.getValue(PortableStorageInterfaceBlock.FACING)
                    .getAxis();
            return bearingAxis != axis;
        });
    }

    @Override
    public ScrollOptionBehaviour<WorkingMode> getWorkingMode() {
        return vs_addition$workingMode;
    }

}
