package com.blockninja.counterclockwise;

import com.mojang.datafixers.util.Pair;
import com.simibubi.create.content.kinetics.turntable.TurntableBlockEntity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3d;
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
import java.util.function.UnaryOperator;

public class TempUtil {

    public record TurntableData(Vector3dc position, Vector3dc axis, double speedRads, List<Long> ships) {}

    public static ConcurrentHashMap<Long, TurntableData> thingy = new ConcurrentHashMap<>();

    public static void doThing(TurntableBlockEntity self) {
        ServerLevel level = (ServerLevel) self.getLevel();
        AABB aabb = self.getRenderBoundingBox().inflate(0.0, 0.1, 0.0);

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

        Iterable<Ship> ships = VSGameUtilsKt.getShipsIntersecting(level, VSGameUtilsKt.transformAabbToWorld(level, aabb));
        ArrayList<Long> ids = new ArrayList<>();
        ships.forEach((ship) -> {
            if (!(ship instanceof ServerShip)) return;
            ids.add(ship.getId());
        });

        Vector3d up = new Vector3d(0, 1, 0);
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, self.getBlockPos());
        if (ship != null) {
            ids.remove(ship.getId());
            ship.getShipToWorld().transformDirection(up);
            pos = ship.getShipToWorld().transformPosition(new Vector3d(pos));
        }
        thingy.put(self.getBlockPos().asLong(), new TurntableData(pos, up, speedRads, ids));
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
                processShip(ship, data.position, data.axis, data.speedRads);
            });
        });
    }


    // This stuff is a bit vibed because I don't understand moment of inertia tensors.
    // I do however understand this code _before_ it got intertia-ified. Check the previous commit
    // for a more artisinal version (that doesn't quite work).
    public static void processShip(PhysShip ship, Vector3dc pos, Vector3dc axis,
                                   double speedRads) {
        final double m = ship.getMass();
        final Vector3dc com = ship.getTransform().getPositionInWorld();
        final Vector3d n = axis.normalize(new Vector3d());   // turntable up, world space
        double dt = 1.0/60.0;

        // Project a vector onto the plane perpendicular to n (in place)
        UnaryOperator<Vector3d> flat =
                v -> v.sub(new Vector3d(n).mul(v.dot(n)));

        // Lever arm: COM -> pivot, in the turntable plane
        Vector3d r = flat.apply(pos.sub(com, new Vector3d()));

        // ---- 1. Drive: tangential target velocity of the COM ----
        // ω × (com - pivot) = ω × (-r), ω = speedRads * n
        Vector3d vTarget = n.cross(r.negate(new Vector3d()), new Vector3d()).mul(speedRads);
        Vector3d dv = flat.apply(vTarget.sub(ship.getVelocity(), new Vector3d()));
        // TODO: cap dv magnitude here

        // ---- 2. Predicted velocity at C, in-plane, using only spin about n ----
        Vector3d t = n.cross(r, new Vector3d());              // tangent: n × r
        double wn = ship.getAngularVelocity().dot(n);
        Vector3d vC = flat.apply(new Vector3d(ship.getVelocity()).add(dv))
                .add(new Vector3d(t).mul(wn));      // ω × r = wn * (n × r)
        // TODO: subtract turntable velocity at C here
        Vector3d b = vC.negate();                             // want Δv_C = -v_C

        // ---- 3. Effective mass terms ----
        Matrix3d iLocalInv = new Matrix3d(ship.getMomentOfInertia()).invert();
        Matrix3d rot = new Matrix3d().set(ship.getTransform().getShipToWorldRotation());
        Matrix3d iWorldInv = rot.mul(iLocalInv, new Matrix3d()).mul(rot.transpose(new Matrix3d()));
        double inn = iWorldInv.transform(new Vector3d(n)).dot(n);   // nᵀ I⁻¹ n

        // ---- 4. Closed-form solve of K J = b, K = (1/m)P + inn * t tᵀ ----
        double denom = 1.0 / m + inn * t.lengthSquared();
        Vector3d J = new Vector3d(b)
                .sub(new Vector3d(t).mul(inn * t.dot(b) / denom))
                .mul(m);

        // ---- 5. Apply ----
        ship.applyWorldForceToBodyPos(dv.mul(m / dt, new Vector3d()), new Vector3d());
        // Apply at the point in the COM's plane so r × J is purely along n
        Vector3d applyPoint = new Vector3d(com).add(r);
        ship.applyWorldForce(J.div(dt, new Vector3d()), applyPoint);
    }

}
