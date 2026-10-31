package com.blockninja.counterclockwise.create.waterwheel

/*
import com.blockninja.counterclockwise.mixin.create.waterwheel.MixinWaterWheelBlockEntity
import com.simibubi.create.content.kinetics.waterwheel.WaterWheelBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import org.joml.AxisAngle4d
import org.joml.Matrix3d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.Ship.transform
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.core.impl.game.ships.ShipInertiaDataImpl
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl.Companion.create
import org.valkyrienskies.core.internal.physics.PhysicsEntityData
import org.valkyrienskies.core.internal.physics.PhysicsEntityServer
import org.valkyrienskies.core.internal.physics.VSSphereCollisionShapeData
import org.valkyrienskies.core.internal.world.VsiServerShipWorld.createPhysicsEntity
import org.valkyrienskies.mod.common.ValkyrienSkiesMod.getOrCreateGTPA
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOML

class WheelHelper {
    @OptIn(GameTickOnly::class)
    fun makeEntity(level: Level, jointId: Int?, pos: BlockPos, facing: Direction) {
        if (level !is ServerLevel) return

        val blockEntity = WaterWheelBlockEntity::class.java.cast(this)

        if (jointId != null) return

        val shipThisIsIn: LoadedServerShip = level.getLoadedShipManagingPos(pos) ?: return

        val newId: Long = level.shipObjectWorld.allocateShipId(level.dimensionId)
        val data = PhysicsEntityData(
            newId,
            create(pos.getCenter().toJOML(), Vector3d(), shipThisIsIn.transform.rotation, Vector3d(1.0, 1.0, 1.0)),
            ShipInertiaDataImpl(Vector3d(), 1.0, sphereInertiaTensor(1.0, 0.5)),
            Vector3d(), Vector3d(),
            VSSphereCollisionShapeData(0.5)
        )

        val entity: PhysicsEntityServer = level.shipObjectWorld.createPhysicsEntity(data, level.dimensionId)



        println(entity)


        // The rotation we apply to different face values. The code below is set up to create Y-hinges by
        // default, and [rotationQuaternion] converts them to other types of hinges
        val rotationQuaternion = when (facing) {
            Direction.DOWN ->  Quaterniond(AxisAngle4d(Math.PI, Vector3d(1.0, 0.0, 0.0)))
            Direction.NORTH -> Quaterniond(AxisAngle4d(Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(Quaterniond(AxisAngle4d(Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)))).normalize()
            Direction.EAST ->  Quaterniond(AxisAngle4d(0.5 * Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(Quaterniond(AxisAngle4d(Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)))).normalize()
            Direction.SOUTH -> Quaterniond(AxisAngle4d(Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0))).normalize()
            Direction.WEST ->  Quaterniond(AxisAngle4d(1.5 * Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(Quaterniond(AxisAngle4d(Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)))).normalize()
            Direction.UP -> Quaterniond()
        }


        // Extra height added to the hinge to keep the top ship slightly above the bottom ship
        val extraHeight = 0.0
        // The positions the hinge attaches relative to the center of mass
        val attachmentOffset0: Vector3dc = rotationQuaternion.transform(Vector3d(0.0, 0.5 + extraHeight, 0.0))
        val attachmentOffset1: Vector3dc = rotationQuaternion.transform(Vector3d(0.0, -0.5, 0.0))
        val worldOffset = Vector3d(0.0, 0.0, 0.0)

        val attachmentLocalPos0: Vector3dc = Vector3d(pos.x.toDouble() + 0.5, pos.y.toDouble() + 0.5, pos.z.toDouble() + 0.5).add(attachmentOffset0).add(worldOffset)
        val attachmentLocalPos1: Vector3dc =
            Vector3d(shipCenterPos.x.toDouble() + 0.5, shipCenterPos.y.toDouble() + 0.5, shipCenterPos.z.toDouble() + 0.5).add(attachmentOffset1)

        // Move [ship] if we are on a ship
        // Put the new ship where the old ship is
        val newPos = shipThisIsIn.transform.shipToWorld.transformPosition(attachmentLocalPos0, Vector3d())
        newPos.sub(shipThisIsIn.transform.shipToWorldRotation.transform(attachmentOffset1, Vector3d()))
        val newKinematics = vsApi.newBodyKinematics(
            shipThisIsIn.velocity,
            shipThisIsIn.angularVelocity,
            newPos,
            shipThisIsIn.transform.shipToWorldRotation, // Copy source ship rotation
            ship.transform.shipToWorldScaling,
            ship.transform.positionInShip,
        )
        // Update the ship transform
        (ship as VsiShip).unsafeSetKinematics(newKinematics)

        level.setBlockAndUpdate(shipCenterPos, Blocks.IRON_BLOCK.defaultBlockState())
        blockEntity.get().otherHingePos = shipCenterPos

        val shipId0 = shipThisIsIn?.id
        val shipId1 = ship.id

        // Hinge constraints will attempt to align the X-axes of both bodies, so to align the Y axis we
        // apply this rotation to the X-axis
        val hingeOrientation = rotationQuaternion.mul(Quaterniond(AxisAngle4d(Math.toRadians(90.0), 0.0, 0.0, 1.0)), Quaterniond()).normalize()

        blockEntity.get().hingeConstraint = VSRevoluteJoint(
            shipId0, VSJointPose(attachmentLocalPos0, hingeOrientation),
            shipId1, VSJointPose(attachmentLocalPos1, hingeOrientation),
            maxForceTorque = null, driveFreeSpin = true
        )
        ValkyrienSkiesMod.getOrCreateGTPA(level.dimensionId).addJoint(blockEntity.get().hingeConstraint!!, delay = 4) { t ->
            blockEntity.get().constraintId = t
        }

        return InteractionResult.CONSUME
    }

    private fun sphereInertiaTensor(mass: Double, radius: Double): Matrix3d {
        val mat = Matrix3d()

        mat.m00 = (2.0 / 5.0) * mass * radius * radius
        mat.m11 = (2.0 / 5.0) * mass * radius * radius
        mat.m22 = (2.0 / 5.0) * mass * radius * radius

        return mat
    }
}*/