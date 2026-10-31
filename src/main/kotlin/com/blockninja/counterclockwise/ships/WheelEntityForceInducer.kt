package com.blockninja.counterclockwise.ships

import com.blockninja.counterclockwise.VSEvents.registeryDimToLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Marker
import net.minecraftforge.server.ServerLifecycleHooks
import org.joml.Vector3d
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ShipPhysicsListener
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.mod.common.util.toMinecraft
import thedarkcolour.kotlinforforge.forge.vectorutil.v3d.times
import thedarkcolour.kotlinforforge.forge.vectorutil.v3d.unaryMinus
import java.util.concurrent.ConcurrentHashMap

class WheelEntityForceInducer: ShipPhysicsListener {
    var rotations: ConcurrentHashMap<Object, Vector3d> = ConcurrentHashMap()

    override fun physTick(physShip: PhysShip, physLevel: PhysLevel) {
        val force = -Vector3d(physShip.velocity) * physShip.mass //* 0.05
        val omega = -Vector3d(physShip.angularVelocity) * physShip.mass //* 0.05

        //physShip.applyInvariantForce(force)
        physShip.applyInvariantTorque(omega)

        rotations.values.forEach {
            physShip.applyInvariantTorque(it)
        }

        val server = ServerLifecycleHooks.getCurrentServer()
        val level = registeryDimToLevel(physLevel.dimension)

        try {
            if (level != null) {

                val pointA = physShip.transform.positionInWorld

                val marker = Marker(EntityType.MARKER, level);
                marker.setPos(pointA.toMinecraft());
                level.addFreshEntity(marker);

            }
        } catch (cme: ConcurrentModificationException) {
            println("Cme wuh oh")
        }
    }
}