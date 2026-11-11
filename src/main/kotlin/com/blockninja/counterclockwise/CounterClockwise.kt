package com.blockninja.counterclockwise

import com.blockninja.counterclockwise.entity.CounterClockwiseEntityTypes
import com.blockninja.counterclockwise.ships.BeltStorageForceInducer
import com.blockninja.counterclockwise.ships.EjectorForceInducer
import com.blockninja.counterclockwise.ships.FanForceInducer
import com.simibubi.create.AllBlocks
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraftforge.fml.common.Mod
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.primitives.AABBd
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.event.RegisteredListener
import org.valkyrienskies.core.api.events.CollisionEvent
import org.valkyrienskies.core.api.events.ShipLoadEvent
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.core.api.util.PhysTickOnly
import org.valkyrienskies.core.api.util.functions.DoubleTernaryConsumer
import org.valkyrienskies.core.util.expand
import org.valkyrienskies.mod.api.events.RegisterBlockStateEvent
import org.valkyrienskies.mod.api.toJOML
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.mod.common.config.MassDatapackResolver.generateShapeFromVoxel
import org.valkyrienskies.mod.common.config.VSGameConfig
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.vsCore
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.forge.vectorutil.v3d.minus
import java.util.function.Consumer
import java.util.stream.Stream
import kotlin.math.abs
import kotlin.math.sqrt


@OptIn(PhysTickOnly::class, VsBeta::class, GameTickOnly::class)
@Mod(CounterClockwise.MOD_ID)
class CounterClockwise {

    init {

        // region Register BlockPos so it can be serialized as a map key
        /*SimpleModule module = new SimpleModule();
        module.addKeySerializer(BlockPos.class, new BlockPosKeySerializer());
        module.addKeyDeserializer(BlockPos.class, new BlockPosKeyDeserializer());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);*/
        // endregion

        vsApi.collisionPersistEvent.on { collisionEvent: CollisionEvent, registeredListener: RegisteredListener ->
            VSEvents.collide(
                collisionEvent,
                registeredListener
            )
        }

        vsApi.registerBlockStateEvent.on((Consumer { registerBlockStateEvent: RegisterBlockStateEvent ->
            var testVoxelShape = Stream.of(
                Block.box(0.0, -5.0, 0.0, 16.0, 21.0, 16.0),
                Block.box(0.0, 0.0, -5.0, 16.0, 16.0, 21.0),
                Block.box(-5.0, 0.0, 0.0, 21.0, 16.0, 16.0)
            ).reduce { v1: VoxelShape, v2: VoxelShape -> Shapes.join(v1, v2, BooleanOp.OR) }.get();

            val shape = generateShapeFromVoxel(testVoxelShape) ?: return@Consumer
            val solidState = vsCore.newSolidStateBuilder()
                .shape(shape)
                .elasticity(VSGameConfig.SERVER.defaultBlockElasticity)
                .friction(VSGameConfig.SERVER.defaultBlockFriction)
                .hardness(VSGameConfig.SERVER.defaultBlockHardness)
                .build()

            registerBlockStateEvent.register(AllBlocks.WATER_WHEEL.get().defaultBlockState(), solidState)
        }))


        vsApi.registerAttachment<BeltStorageForceInducer>(
            vsApi
                .newAttachmentRegistrationBuilder(BeltStorageForceInducer::class.java)
                .useJacksonSerializer()
                .build()
            )

        vsApi.registerAttachment<FanForceInducer>(
            vsApi
                .newAttachmentRegistrationBuilder(FanForceInducer::class.java)
                .useJacksonSerializer()
                .build()
        )

        vsApi.registerAttachment(EjectorForceInducer::class.java)

        vsApi.shipLoadEvent.on(Consumer { event: ShipLoadEvent ->
            event.ship.setAttachment<BeltStorageForceInducer>(BeltStorageForceInducer())
        })

        vsApi.physTickEvent.on { event ->
            TempUtil.onPhysTick(event.world)
        }

        CounterClockwiseEntityTypes.register(MOD_BUS)
    }

    companion object {
        const val MOD_ID: String = "counterclockwise"
    }
}

fun BlockPos.centerJOMLD(): Vector3d {
    return this.center.toJOML()
}

//https://stackoverflow.com/questions/1171849/finding-quaternion-representing-the-rotation-from-one-vector-to-another
fun getHingeRotation(localDir: Vector3d, right: Vector3d = Vector3d(1.0, 0.0, 0.0)): Quaterniond {
    if ((localDir - right).dist() < 1e-5) { return Quaterniond() }

    val v1l = right.dist()
    val v2l = localDir.dist()

    val a = right.cross(localDir)

    val k = sqrt(v1l * v1l * v2l * v2l)
    val kCosTheta = right.dot(localDir)

    if (abs(kCosTheta / k + 1.0) < 1e-5) {
        val ort = right.let { it.orthogonalize(it) }
        return Quaterniond(ort.x, ort.y, ort.z, 0.0).normalize()
    }

    return Quaterniond(a.x, a.y, a.z, k + kCosTheta).normalize()
}

fun Vector3d.dist(): Double {return sqrt(x * x + y * y + z * z)}

fun Level?.transformToNearbyShipsAndWorldFixed(
    x: Double, y: Double, z: Double, aabbRadius: Double, cb: DoubleTernaryConsumer
) {
    this?.transformToNearbyShipsAndWorldFixed(x, y, z, aabbRadius, cb::accept)
}

inline fun Level.transformToNearbyShipsAndWorldFixed(
    x: Double, y: Double, z: Double, aabbRadius: Double, cb: (Double, Double, Double) -> Unit
) {
    val currentShip = getShipManagingPos(x, y, z)

    val posInWorld = Vector3d(x, y, z)
    val temp0 = Vector3d()

    if (currentShip != null) {
        currentShip.shipToWorld.transformPosition(posInWorld)

        cb(posInWorld.x(), posInWorld.y(), posInWorld.z())
    }

    val aabb = AABBd(posInWorld.x, posInWorld.y, posInWorld.z, posInWorld.x, posInWorld.y, posInWorld.z).expand(aabbRadius)

    for (nearbyShip in shipObjectWorld.allShips.getIntersecting(aabb, this!!.dimensionId)) {
        if (nearbyShip == currentShip) continue
        val posInShip = nearbyShip.worldToShip.transformPosition(posInWorld, temp0)
        cb(posInShip.x(), posInShip.y(), posInShip.z())
    }
}