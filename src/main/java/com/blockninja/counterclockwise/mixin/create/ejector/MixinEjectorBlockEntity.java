package com.blockninja.counterclockwise.mixin.create.ejector;

import com.blockninja.counterclockwise.ships.EjectorForceInducer;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.depot.EjectorBlockEntity;
import com.simibubi.create.content.logistics.depot.EntityLauncher;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.checkerframework.checker.units.qual.A;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.util.GameToPhysicsAdapter;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

@Mixin(EjectorBlockEntity.class)
public abstract class MixinEjectorBlockEntity extends SyncedBlockEntity {

    public MixinEjectorBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    @Shadow EntityLauncher launcher;

    @Shadow
    protected Direction getFacing() { return null; }

    @Shadow
    public abstract void activate();

    @Shadow
    private boolean powered;

    @Shadow
    public abstract void initialize();

    @Inject(
            method = "ejectIfTriggered",
            at = @At("HEAD"),
            remap = false,
            cancellable = true
    )
    private void injectTrigger(CallbackInfo ci) {
        if (powered)
            return;

        AABB aabb = new AABB(worldPosition).inflate(-1 / 16f, 0, -1 / 16f);

        if (level == null) return;
        Iterable<Ship> ships = VSGameUtilsKt.getShipsIntersecting(level, aabb);
        if (ships.iterator().hasNext()) {
            activate();
            notifyUpdate();
            ci.cancel();
        }
    }

    @Inject(
            method = "activateDeferred",
            at = @At("RETURN"),
            remap = false
    )
    private void injectActivate(CallbackInfo ci) {
        Direction facing = getFacing();
        AABB aabb = new AABB(worldPosition).inflate(-1 / 16f, 0, -1 / 16f);

        if (level == null) return;
        if (!(level instanceof ServerLevel)) return;
        Iterable<Ship> ships = VSGameUtilsKt.getShipsIntersecting(level, aabb);

        Vector3dc motion = VectorConversionsMCKt.toJOML(getMotion((EntityLauncherAccessor) launcher, facing)).mul(20.0);
        GameToPhysicsAdapter gtpa = ValkyrienSkiesMod.getOrCreateGTPA(VSGameUtilsKt.getDimensionId(level));

        ships.forEach((ship) -> {
            if (!(ship instanceof ServerShip ss)) return;
            LoadedShip loadedS = VSGameUtilsKt.getShipObjectWorld(level).getLoadedShips().getById(ss.getId());
            if (!(loadedS instanceof LoadedServerShip loadedSS)) return;

            Vector3d motionScaled = motion.mul(ss.getInertiaData().getMass() / (1.0 / 60.0), new Vector3d());
            EjectorForceInducer drag = EjectorForceInducer.getOrCreate(loadedSS);
            drag.setStepsLeft((int) Math.ceil(3 * (launcher.getTotalFlyingTicks()+20)));

            gtpa.applyWorldForce(ss.getId(), motionScaled, null);
        });

    }

    public Vec3 getMotion(EntityLauncherAccessor launcher, Direction facing) {
        Vec3 motionVec = new Vec3(0, launcher.ccw$getYMotion(), launcher.ccw$getXMotion());
        motionVec = VecHelper.rotate(motionVec, AngleHelper.horizontalAngle(facing), Direction.Axis.Y);
        return new Vec3(motionVec.x * .91, motionVec.y * .98, motionVec.z * .91);
    }
}
