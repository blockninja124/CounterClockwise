package com.blockninja.counterclockwise;

import com.mojang.datafixers.util.Pair;
import com.simibubi.create.content.kinetics.turntable.TurntableBlockEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.world.PhysLevel;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.util.GameToPhysicsAdapter;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.concurrent.ConcurrentHashMap;

public class TempUtil {
    /*public static void doThing(TurntableBlockEntity self) {
        ServerLevel level = (ServerLevel) self.getLevel();
        AABB aabb = self.getRenderBoundingBox().inflate(0.0, 0.5, 0.0);

        // RPM
        float speed = self.getSpeed();
        if (speed == 0)
            return;

        double outsideVelocity = 2.0*Math.PI*(1.0/2.0)*(speed/60.0);
        //System.out.println("Outside vel: ");
        //System.out.println(outsideVelocity);
        // /vs apply torque diaper-aunt-auto (0 20000000 0) world
        GameToPhysicsAdapter gtpa = ValkyrienSkiesMod.getOrCreateGTPA(VSGameUtilsKt.getDimensionId(level));

        Vector3dc pos = VectorConversionsMCKt.toJOML(self.getBlockPos().getCenter());

        Iterable<Ship> ships = VSGameUtilsKt.getShipsIntersecting(level, aabb);
        ships.forEach(ship -> {
            if (!(ship instanceof ServerShip serverShip)) return;

            //if (serverShip.getAngularVelocity().y() < 1e-4) {
            Vector3dc newPos = ship.getWorldToShip().transformPosition(pos, new Vector3d());

            double shipOutsideVelocity = 2.0*Math.PI*(newPos.distance(serverShip.getTransform().getPositionInModel()))*(radsToRPM(serverShip.getAngularVelocity().y())/60.0);
            double desiredShipOutsideVelocity = 2.0*Math.PI*(newPos.distance(serverShip.getTransform().getPositionInModel()))*(speed/60.0);


            Vector3dc vecToCenter = serverShip.getTransform().getShipToWorld().transformDirection(serverShip.getTransform().getPositionInModel().sub(newPos, new Vector3d()));

            for (int i = 0; i < 10; i++) {
                double factor = i/5.0;
                Vector3d halfPos = vecToCenter.mul(factor, new Vector3d()).add(pos);
                level.sendParticles(
                        ParticleTypes.CRIT,
                        halfPos.x, halfPos.y, halfPos.z,
                        1,       // particle count
                        0, 0, 0, // position spread
                        0.0      // speed
                );
            }

            // tan^-1(45)
            double tanFactor = 88.7269699799;

            // 90 degrees

            //Vector3dc tanVec = vecToCenter.add(0.0, -vecToCenter.y(), 0.0, new Vector3d()).mul(tanFactor).normalize().mul(tanFactor);
            Vector3dc tanVec = new Vector3d(
                    vecToCenter.z(),
                    0,
                    -vecToCenter.x()
            ).normalize();

            gtpa.applyWorldForceToBodyPos(ship.getId(), tanVec.mul(desiredShipOutsideVelocity - shipOutsideVelocity, new Vector3d()).mul(serverShip.getInertiaData().getMass()).mul(1.0), new Vector3d());
            gtpa. (ship.getId(), tanVec.mul(outsideVelocity - shipOutsideVelocity, new Vector3d()).mul(serverShip.getInertiaData().getMass()).mul(1.0), newPos);


            for (int i = 0; i < 10; i++) {
                double factor = i/5.0;
                Vector3d halfPos = tanVec.mul(factor, new Vector3d()).add(pos);
                level.sendParticles(
                        ParticleTypes.FLAME,
                        halfPos.x, halfPos.y, halfPos.z,
                        1,       // particle count
                        0, 0, 0, // position spread
                        0.0      // speed
                );
            }

            //System.out.println("Ship outside vel: ");
            //System.out.println(shipOutsideVelocity);
            //gtpa.appltorq(ship.getId(), new Vector3d(0, speed*1000000, 0), newPos);
            //}

        });
    }*/

    public static ConcurrentHashMap<Long, Pair<Vector3d, Vector3d>> thingy = new ConcurrentHashMap<>();

    public static void doThing(TurntableBlockEntity self) {
        if (!(self.getLevel() instanceof ServerLevel level)) return;

        // TODO: This will break w/ more than one turntable
        thingy.clear();

        double rpm = self.getSpeed();
        if (rpm == 0) return;
        double omega = rpm * 2.0 * Math.PI / 60.0;           // rad/s about +Y

        AABB aabb = self.getRenderBoundingBox().inflate(0.0, 0.5, 0.0);
        GameToPhysicsAdapter gtpa = ValkyrienSkiesMod.getOrCreateGTPA(VSGameUtilsKt.getDimensionId(level));
        Vector3dc axisPos = VectorConversionsMCKt.toJOML(self.getBlockPos().getCenter()); // convert to world if on a ship
        final double GAIN = 10.0; // tune

        for (Ship ship : VSGameUtilsKt.getShipsIntersecting(level, aabb)) {
            if (!(ship instanceof ServerShip serverShip)) continue;

            serverShip.getInertiaData().getMomentOfInertiaTensor()

            // COM relative to turntable axis, in world space, horizontal only
            Vector3dc com = serverShip.getTransform().getPositionInWorld();
            Vector3d r = new Vector3d(com.x() - axisPos.x(), 0, com.z() - axisPos.z());

            double mass = serverShip.getInertiaData().getMass();

            // 1) COM should orbit the axis: v = ω ŷ × r
            Vector3d vDesired = new Vector3d(omega * r.z, 0, -omega * r.x);
            Vector3dc v = serverShip.getVelocity();
            Vector3d dv = new Vector3d(vDesired.x - v.x(), 0, vDesired.z - v.z());
            Vector3d force = dv.mul(mass * GAIN);

            // 2) Ship should spin at ω about its own COM
            double iyy = serverShip.getInertiaData().getMomentOfInertiaTensor().m11(); // approx, assumes ship is upright
            double dOmega = omega - serverShip.getAngularVelocity().y();
            Vector3d torque = new Vector3d(0, iyy * dOmega * GAIN, 0);

            thingy.put(ship.getId(), new Pair<>(force, torque));

        }
    }

    private static double radsToRPM(double radsPerSec) {
        return radsPerSec * 60.0 / (2.0 * Math.PI);
    }

    public static void onPhysTick(PhysLevel physLevel) {
        thingy.forEach((id, pair) -> {
            if (physLevel.getShipById(id) == null) return;
            physLevel.getShipById(id).applyWorldForceToBodyPos(pair.getFirst(), new Vector3d());
            physLevel.getShipById(id).applyWorldTorque(pair.getSecond());
        });
    }
}
