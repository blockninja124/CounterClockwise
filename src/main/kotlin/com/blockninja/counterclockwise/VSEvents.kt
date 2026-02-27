package com.blockninja.counterclockwise

import com.blockninja.counterclockwise.mixin.PhysShipAttachmentAccessor
import com.blockninja.counterclockwise.ships.BeltStorageForceInducer
import com.mojang.datafixers.util.Pair
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.api.event.RegisteredListener
import org.valkyrienskies.core.api.events.CollisionEvent
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.core.api.util.PhysTickOnly
import org.valkyrienskies.mod.common.ValkyrienSkiesMod
import org.valkyrienskies.mod.common.util.toMinecraft
import kotlin.math.abs

object VSEvents {

    // Physics tick only my ass :trollface:
    @OptIn(PhysTickOnly::class, GameTickOnly::class)
    fun collide(collisionEvent: CollisionEvent, registeredListener: RegisteredListener) {
        val shipA = collisionEvent.physLevel.getShipById(collisionEvent.shipIdA)
        val shipB = collisionEvent.physLevel.getShipById(collisionEvent.shipIdB)

        // TODO: handle multiple ships later
        val mainShip = shipA ?: shipB

        // Shouldn't ever happen, would be a world-world collision
        if (mainShip == null) return

        // val server = ServerLifecycleHooks.getCurrentServer()

        val level = registeryDimToLevel(collisionEvent.dimensionId)

        try {
            if (level != null) {
                for (point in collisionEvent.contactPoints) {
                    val pointA = point.position.toMinecraft()

                    /*Entity marker = new Marker(EntityType.MARKER, level);
                    marker.setPos(pointA);
                    level.addFreshEntity(marker);*/
                }
            }
        } catch (cme: ConcurrentModificationException) {
            println("Cme wuh oh")
        }

        // should be better than try catching a ConcurrentModificationException right? even if it has to iterate through
        // every attachment.
        val physicsListeners = (mainShip as PhysShipAttachmentAccessor).physicsListeners
        var inducer : BeltStorageForceInducer? = null

        for (listener in physicsListeners) {
            if (listener is BeltStorageForceInducer) {
                inducer = listener
            }
        }

        if (inducer != null) {
            val matchingContacts: MutableList<Pair<Vector3dc, BeltStorageForceInducer.BeltData>> =
                ArrayList<Pair<Vector3dc, BeltStorageForceInducer.BeltData>>()

            for (contact in collisionEvent.contactPoints) {
                val contactWorldPos = contact.position

                for (blockPosLong in inducer.beltLocations.keys) {
                    // Belt is in shipyard, but collision is in world, so we transform world -> ship to check against belt AABB
                    val shipCollisionPos =
                        mainShip.transform.worldToShip.transformPosition(contactWorldPos, Vector3d())

                    // TODO: replace with collision shape
                    val beltPos = BlockPos.of(blockPosLong)
                    var beltAABB = AABB(
                        beltPos.x.toDouble(),
                        beltPos.y.toDouble(),
                        beltPos.z.toDouble(),
                        (beltPos.x + 1).toDouble(),
                        (beltPos.y + 1).toDouble(),
                        (beltPos.z + 1).toDouble()
                    )
                    beltAABB = beltAABB.inflate(0.1)

                    if (beltAABB.contains(shipCollisionPos.toMinecraft())) {
                        println("e")
                        matchingContacts.add(
                            Pair<Vector3dc, BeltStorageForceInducer.BeltData>(
                                contactWorldPos,
                                inducer.beltLocations.get(blockPosLong)
                            )
                        )
                    }
                }
            }

            for (pair in matchingContacts) {
                // Collision point world -> ship
                val forcePos = Vector3d(pair.getFirst())
                mainShip.transform.worldToShip.transformPosition(forcePos)

                // Collision point ship -> COM offset
                forcePos.add(0.5, 0.5, 0.5)
                    .sub(mainShip.transform.positionInShip)

                val forceDir = pair.getSecond()!!.direction.step()
                mainShip.transform.shipToWorld.transformDirection(forceDir)

                mainShip.applyInvariantForceToPos(
                    Vector3d(forceDir).mul(mainShip.mass).mul((abs(pair.getSecond()!!.speed) * 48).toDouble()),
                    forcePos
                )
            }
        }


        // check if belt
        //if (!AllBlocks.BELT.has(level.getBlockState(worldPosition)))
        //    return;
    }

    /**
     * Get [ServerLevel][net.minecraft.server.level.ServerLevel] from a VS dimension ID.
     *
     * @param dimension The dimension ID string in format registry_namespace:registry_name:dimension_namespace:dimension_name
     * @return A [ServerLevel][net.minecraft.server.level.ServerLevel] instance with the dimension ID given
     */
    fun registeryDimToLevel(dimension: String): ServerLevel? {
        // Split 'minecraft:dimension:namespace:dimension_name' into [minecraft, dimension, namespace, dimension_name]
        val parts: Array<String> = dimension.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        require(parts.size == 4) { "Unexpected dimension ID: " + dimension }
        val levelId: ResourceKey<Level> = ResourceKey.create(
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(parts[0], parts[1])),
            ResourceLocation.fromNamespaceAndPath(parts[2], parts[3])
        )
        return ValkyrienSkiesMod.currentServer?.getLevel(levelId)
    }
}