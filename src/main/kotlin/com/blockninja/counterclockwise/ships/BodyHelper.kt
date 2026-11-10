package com.blockninja.counterclockwise.ships
/*
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.core.api.bodies.ServerVsBody
import org.valkyrienskies.core.api.bodies.VsBody
import org.valkyrienskies.core.api.bodies.properties.BodySegmentId
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.core.impl.game.bodies.VsBodyData
import org.valkyrienskies.mod.api.toJOML
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.mod.common.allBodies
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.vsCore
import java.util.function.Predicate

object BodyHelper {
    @OptIn(GameTickOnly::class)
    fun addCollisionToShip(pos: BlockPos, serverLevel: ServerLevel): BodySegmentId? {
        val ship = serverLevel.getLoadedShipManagingPos(pos) ?: return null

        val center = pos.center.toJOML()
        val id = ship.addCollisionSegment(
            vsCore.newBoxBodyShape(Vector3d(0.5, 0.5, 0.5)),
            center,
            Quaterniond(),
        )

        ship.setCollisionSegmentTransform(
            id,
            center,
            Quaterniond(),
            Vector3d(10.0, 0.0, 0.0),
            Vector3d(0.0, 0.0, 0.0)
        )


        serverLevel.shipObjectWorld.dimensionToGroundBodyIdImmutable

        return id
    }

    @OptIn(GameTickOnly::class)
    fun removeCollisionFromShip(pos: BlockPos, serverLevel: ServerLevel, segmentId: BodySegmentId) {
        val ship = serverLevel.getLoadedShipManagingPos(pos) ?: return

        ship.removeCollisionSegment(segmentId)
    }
}*/