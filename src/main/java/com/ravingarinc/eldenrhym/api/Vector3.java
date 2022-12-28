package com.ravingarinc.eldenrhym.api;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import javax.annotation.concurrent.ThreadSafe;
import javax.vecmath.Vector3d;

/**
 * Vector safe to use in asynchronous applications. Also has useful methods.
 */
@ThreadSafe
public class Vector3 extends Vector3d {
    private final float yaw;
    private final float pitch;

    public Vector3(final Location location) {
        this(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public Vector3(final Vector vector) {
        this(vector.getX(), vector.getY(), vector.getZ(), 0, 0);
    }

    public Vector3() {
        this(0, 0, 0, 0, 0);
    }

    public Vector3(final double x, final double y, final double z) {
        this(x, y, z, 0f, 0f);
    }

    public Vector3(final double x, final double y, final double z, final float yaw, final float pitch) {
        super(x, y, z);
        this.yaw = yaw;
        this.pitch = pitch;
    }

    /**
     * Calculates the distance between this vector and the provided vector. This value is cached
     * and only recalculated if the vector's provided have different values.
     *
     * @param to The vector to
     * @return The distance
     */
    public double distance(final Vector3 to) {
        return Math.sqrt(square(this.x - to.x) + square(this.y - to.y) + square(this.z - to.z));
    }

    private double square(final double num) {
        return num * num;
    }

    /**
     * Calculates direction based on yaw / pitch value. Yaw and pitch may not be set in all cases.
     *
     * @return The direction.
     */
    public Vector3 getDirection() {
        final Vector3 vector = new Vector3();
        final double rotX = yaw;
        final double rotY = pitch;

        vector.setY(-Math.sin(Math.toRadians(rotY)));

        final double xz = Math.cos(Math.toRadians(rotY));

        vector.setX(-xz * Math.sin(Math.toRadians(rotX)));
        vector.setZ(xz * Math.cos(Math.toRadians(rotX)));

        return vector;
    }

    /**
     * Create a Bukkit Vector object.
     *
     * @return vector
     */
    public Vector toBukkitVector() {
        return new Vector(this.getX(), this.getY(), this.getZ());
    }

    @Override
    public String toString() {
        return "x = " + x + ", y = " + y + ", z = " + z;
    }
}
