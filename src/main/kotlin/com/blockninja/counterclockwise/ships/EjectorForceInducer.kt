package com.blockninja.counterclockwise.ships

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import org.joml.Vector3d
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ShipPhysicsListener
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import kotlin.concurrent.Volatile

/**
 * Used to mimic the high drag that entities have but for ships so that their ejector trajectory is (mostly) the same
 */
class EjectorForceInducer : ShipPhysicsListener {
    @Volatile
    var stepsLeft: Int = 0
    var kHorizontal: Double = 1.89
    var kVertical: Double = 0.40

    override fun physTick(physShip: PhysShip, physLevel: PhysLevel) {
        if (stepsLeft <= 0) return
        stepsLeft--

        val v = physShip.velocity
        val m = physShip.mass

        val drag = Vector3d(
            -v.x() * kHorizontal * m,
            -v.y() * kHorizontal * m,
            -v.z() * kHorizontal * m
        )

        physShip.applyWorldForce(drag)
    }

    companion object {
        @OptIn(GameTickOnly::class)
        @JvmStatic
        fun getOrCreate(ship: LoadedServerShip): EjectorForceInducer {
            var attachment = ship.getAttachment(EjectorForceInducer::class.java)
            if (attachment == null) {
                attachment = EjectorForceInducer()
                ship.setAttachment(attachment)
            }
            return attachment
        }

        @OptIn(GameTickOnly::class)
        fun get(level: Level, pos: BlockPos): EjectorForceInducer? {
            val serverLevel = level as ServerLevel?

            val ship = serverLevel.getLoadedShipManagingPos(pos)

            return if (ship != null) getOrCreate(ship) else null
        }
    }
}