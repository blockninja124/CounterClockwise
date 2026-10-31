package com.blockninja.counterclockwise.mixin.create.belt_tracks;

import com.blockninja.counterclockwise.mixinducks.create.RemoveBody;
import com.blockninja.counterclockwise.ships.BeltStorageForceInducer;
import com.blockninja.counterclockwise.ships.BodyHelper;
import com.blockninja.counterclockwise.ships.WheelEntityForceInducer;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.checkerframework.common.aliasing.qual.Unique;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.bodies.ServerVsBody;
import org.valkyrienskies.core.api.bodies.VsBody;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.world.PhysLevel;
import org.valkyrienskies.core.impl.game.ships.ShipInertiaDataImpl;
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl;
import org.valkyrienskies.core.internal.joints.VSD6Joint;
import org.valkyrienskies.core.internal.joints.VSJoint;
import org.valkyrienskies.core.internal.joints.VSJointPose;
import org.valkyrienskies.core.internal.joints.VSRevoluteJoint;
import org.valkyrienskies.core.internal.physics.PhysicsEntityData;
import org.valkyrienskies.core.internal.physics.PhysicsEntityServer;
import org.valkyrienskies.core.internal.physics.RigidBodyDefaults;
import org.valkyrienskies.core.internal.physics.VSSphereCollisionShapeData;
import org.valkyrienskies.mod.api.BlockEntityPhysicsListener;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.util.GameToPhysicsAdapter;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.blockninja.counterclockwise.CounterClockwiseKt.getHingeRotation;

@Mixin(BeltBlockEntity.class)
public abstract class MixinBeltBlockEntity extends BlockEntity implements RemoveBody {

    @Shadow
    public abstract float getBeltMovementSpeed();

    @Shadow
    public abstract float getDirectionAwareBeltMovementSpeed();

    @Shadow
    public abstract Direction getMovementFacing();

    @Unique
    private int entityId = -1;

    public MixinBeltBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    @Inject(
            method = "tick",
            at = @At("RETURN"),
            remap = false
    )
    private void injectTick(CallbackInfo ci) {
        if (!AllBlocks.BELT.has(level.getBlockState(worldPosition))) {
            removeBody();
            return;
        }

        //System.out.println(getDirectionAwareBeltMovementSpeed());

        if (level instanceof ServerLevel serverLevel) {
            if (entityId == -1) {
                var newId = BodyHelper.INSTANCE.addCollisionToShip(getBlockPos(), serverLevel);
                if (newId != null) {
                    entityId = newId;
                }
            }

            /*LoadedServerShip ship = VSGameUtilsKt.getLoadedShipManagingPos(serverLevel, worldPosition);
            if (ship == null) return;

            BeltStorageForceInducer inducer = BeltStorageForceInducer.getOrCreate(ship);
            inducer.beltLocations.put(worldPosition.asLong(), new BeltStorageForceInducer.BeltData(
                    getBlockState().getValue(BeltBlock.SLOPE),
                    getBlockState().getValue(BeltBlock.PART),
                    getBeltMovementSpeed(),
                    getMovementFacing()
            ));*/

            /*if (entityId == null) {
                long newId = VSGameUtilsKt.getShipObjectWorld(serverLevel).allocateShipId(VSGameUtilsKt.getDimensionId(serverLevel));

                PhysicsEntityServer entity = VSGameUtilsKt.getShipObjectWorld(serverLevel).createPhysicsEntity(new PhysicsEntityData(
                        newId,
                        ShipTransformImpl.Companion.create(
                                VectorConversionsMCKt.toJOML(worldPosition.getCenter()), //pos
                                new Vector3d(), // com pos
                                new Quaterniond(), // ship rot
                                new Vector3d(1.0, 1.0, 1.0) // scale
                        ),
                        new ShipInertiaDataImpl(new Vector3d(), 1.0, sphereInertiaTensor(1.0, 0.6)),
                        new Vector3d(), new Vector3d(),
                        new VSSphereCollisionShapeData(0.6),
                        RigidBodyDefaults.DEFAULT_COLLISION_MASK,
                        RigidBodyDefaults.DEFAULT_STATIC_FRICTION_COEFFICIENT,
                        RigidBodyDefaults.DEFAULT_DYNAMIC_FRICTION_COEFFICIENT,
                        RigidBodyDefaults.DEFAULT_RESTITUTION_COEFFICIENT,
                        false
                ), VSGameUtilsKt.getDimensionId(serverLevel));

                // TODO: use a revolute joint to hold the physics entity in place
                // then just use the phys tick listener (possibly in this BEPL instead of an attachment)
                // to apply a rotational force based on rpm.
                WheelEntityForceInducer inducer = new WheelEntityForceInducer();
                ConcurrentHashMap map = new ConcurrentHashMap();
                map.put(0, new Vector3d(0.0, 5.0, 0.0));
                inducer.setRotations(map);

                entity.getPhysicsListeners().add(inducer);

                Quaterniondc rotA = getHingeRotation(new Vector3d(0.0, 1.0, 0.0), new Vector3d(1.0, 0.0, 0.0));
                Quaterniondc rotB = getHingeRotation((new Vector3d(0.0, 1.0, 0.0).negate()), new Vector3d(1.0, 0.0, 0.0));
                VSRevoluteJoint joint = makeRevoluteJoint(null, entity.getId(), rotA, rotB, worldPosition.getCenter(), new Vec3(0, 0, 0), null, null);
                addJoint(level, joint, (id) -> {});

                entityId = newId;*/

        }

    }

    @Override
    public void removeBody() {
        if (entityId == -1) return;
        if (level instanceof ServerLevel serverLevel) {
            BodyHelper.INSTANCE.removeCollisionFromShip(getBlockPos(), serverLevel, entityId);
            entityId = -1;
        }
    }
}
