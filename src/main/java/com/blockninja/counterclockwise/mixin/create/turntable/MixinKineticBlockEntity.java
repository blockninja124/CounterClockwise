package com.blockninja.counterclockwise.mixin.create.turntable;

import com.blockninja.counterclockwise.TempUtil;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.turntable.TurntableBlockEntity;
import net.minecraft.client.particle.FlameParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.util.GameToPhysicsAdapter;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

@Mixin(KineticBlockEntity.class)
public class MixinKineticBlockEntity {

    @Inject(
            method = "tick",
            at = @At("HEAD"),
            remap = false
    )
    private void tick(CallbackInfo ci) {
        if (!((Object) this instanceof TurntableBlockEntity self)) return;

        if (self.getLevel().isClientSide) return;

        TempUtil.doThing(self);

    }


}
