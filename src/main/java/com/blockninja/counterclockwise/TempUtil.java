package com.blockninja.counterclockwise;

import com.mojang.datafixers.util.Pair;
import com.simibubi.create.content.kinetics.turntable.TurntableBlockEntity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.world.PhysLevel;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.util.GameToPhysicsAdapter;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TempUtil {

    public record TurntableData(Vector3dc position, double speedRads, List<Long> ships) {}

    public static ConcurrentHashMap<Long, TurntableData> thingy = new ConcurrentHashMap<>();

    public static void doThing(TurntableBlockEntity self) {
        ServerLevel level = (ServerLevel) self.getLevel();
        AABB aabb = self.getRenderBoundingBox().inflate(0.0, 0.5, 0.0);

        // RPM
        float speed = self.getSpeed();
        if (speed == 0)
            return;

        double speedRads = rpmToRads(speed);
        // 2.0*Math.PI*(1.0/2.0)*(speed/60.0);
        //System.out.println("Outside vel: ");
        //System.out.println(outsideVelocity);
        // /vs apply torque diaper-aunt-auto (0 20000000 0) world

        Vector3dc pos = VectorConversionsMCKt.toJOML(self.getBlockPos().getCenter());

        Iterable<Ship> ships = VSGameUtilsKt.getShipsIntersecting(level, aabb);
        ArrayList<Long> ids = new ArrayList<>();
        ships.forEach((ship) -> {
            if (!(ship instanceof ServerShip)) return;
            ids.add(ship.getId());
        });

        thingy.put(self.getBlockPos().asLong(), new TurntableData(pos, speedRads, ids));
    }

    public static void debugVector(ServerLevel level, Vector3dc vec, Vector3dc position, ParticleOptions type) {
        for (int i = 0; i < 10; i++) {
            double factor = i/5.0;
            Vector3d halfPos = vec.mul(factor, new Vector3d()).add(position);
            level.sendParticles(
                    type,
                    halfPos.x, halfPos.y, halfPos.z,
                    1,       // particle count
                    0, 0, 0, // position spread
                    0.0      // speed
            );
        }
    }

    public static double rpmToRads(float rpm) {
        return rpm * (2.0*Math.PI/60.0);
    }

    public static void onPhysTick(PhysLevel physLevel) {
        thingy.forEach((l, data) -> {
            data.ships.forEach((shipId) -> {
                PhysShip ship = physLevel.getShipById(shipId);
                if (ship == null) return;
                processShip(ship, data.position, data.speedRads);
            });
        });
    }


    public static void processShip(PhysShip ship, Vector3dc pos, double speedRads) {
        Vector3dc com = ship.getTransform().getPositionInWorld();
        com = new Vector3d(com.x(), pos.y(), com.z());

        double distance = pos.distance(com);

        // TODO: Cap f1
        // Note: f1 is already scaled by distance, so we only mult it by rads
        Vector3dc f1 = new Vector3d(0, 1, 0).cross(com.sub(pos, new Vector3d()));//
        f1 = f1.normalize(new Vector3d());
        if (!f1.isFinite()) {
            f1 = new Vector3d(0, 0, 0);
        }
        f1 = f1.mul(distance, new Vector3d()).mul(speedRads);
        f1 = f1.sub(ship.getVelocity(), new Vector3d());
        //debugVector(level, f1, com, ParticleTypes.FLAME);
        ship.applyWorldForceToBodyPos(f1.mul(ship.getMass(), new Vector3d()), new Vector3d());


        double thing = ship.getVelocity().dot(com.sub(pos, new Vector3d()));

        if (thing != 0) {
            Vector3dc outVel = com.sub(pos, new Vector3d()).normalize().negate().mul(thing).mul(ship.getMass());
            System.out.println(outVel);
            //debugVector(level, outVel, com, ParticleTypes.CRIT);
            //ship.applyWorldForceToBodyPos(outVel.mul(1.0, new Vector3d()), new Vector3d());
        }

        // Get velocity at C
        // Linear velocity
        Vector3d velocity = new Vector3d(ship.getVelocity()).add(f1);
        // Add angular velocity (with distance from com accounted for)
        velocity.add(ship.getAngularVelocity().cross(pos.sub(com, new Vector3d()), new Vector3d()));
        // Relative to turntable
        // TODO: turntable ship velocity
        Vector3dc turntableVelocity = new Vector3d(0, 0, 0);
        velocity.sub(turntableVelocity);

        //debugVector(level, velocity, pos, ParticleTypes.CRIT);

        ship.applyWorldForce(velocity.negate().mul(ship.getMass()), pos);
    }

}
