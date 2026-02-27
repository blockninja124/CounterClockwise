package com.blockninja.counterclockwise.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.valkyrienskies.core.api.ships.ShipPhysicsListener;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;

import java.util.List;

@Mixin(PhysShipImpl.class)
public interface PhysShipAttachmentAccessor {
    @Accessor("physicsListeners")
    List<? extends ShipPhysicsListener> getPhysicsListeners();
}
