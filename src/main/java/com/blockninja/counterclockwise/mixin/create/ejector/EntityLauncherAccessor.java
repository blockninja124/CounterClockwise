package com.blockninja.counterclockwise.mixin.create.ejector;

import com.simibubi.create.content.logistics.depot.EntityLauncher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityLauncher.class)
public interface EntityLauncherAccessor {
    @Accessor("yMotion")
    double ccw$getYMotion();

    @Accessor("xMotion")
    double ccw$getXMotion();
}
