package com.blockninja.counterclockwise.mixin.create.psi;

import com.blockninja.counterclockwise.mixinducks.create.portable_interface.IPSIWithShipBehavior;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlockEntity;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceMovement;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PortableStorageInterfaceMovement.class)
public abstract class MixinPortableStorageInterfaceMovement implements MovementBehaviour {

    @WrapOperation(
            method = "findInterface",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/contraptions/actors/psi/PortableStorageInterfaceBlockEntity;isPowered()Z"
            ),
            remap = false
    )
    public boolean findStationaryInterface(PortableStorageInterfaceBlockEntity instance, Operation<Boolean> original, @Local(ordinal = 0, argsOnly = true) MovementContext context) {
        if (instance instanceof IPSIWithShipBehavior behavior && behavior.getWorkingMode().get() == IPSIWithShipBehavior.WorkingMode.WITH_SHIP) {
            return false;
        } else {
            return original.call(instance);
        }
    }

}
