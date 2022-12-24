package com.ravingarinc.eldenrhym.api;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.ThreadSafe;
import javax.vecmath.Vector3d;
import java.util.HashMap;
import java.util.Map;

/**
 * Vector safe to use in asynchronous applications. Also has useful methods.
 */
@ThreadSafe
public class Vector3 extends Vector3d {
    private static final DistanceCache DISTANCE_CACHE = new DistanceCache();

    public Vector3() {
        super();
    }

    public Vector3(final double x, final double y, final double z) {
        super(x, y, z);
    }

    /**
     * Calculates the distance between this vector and the provided vector. This value is cached
     * and only recalculated if the vector's provided have different values.
     *
     * @param to The vector to
     * @return The distance
     */
    public double distance(final Vector3 to) {
        return DISTANCE_CACHE.distance(this, to);
    }

    /**
     * Create a Bukkit Vector object.
     *
     * @return vector
     */
    public Vector toBukkitVector() {
        return new Vector(this.getX(), this.getY(), this.getZ());
    }

    @ThreadSafe
    private static class DistanceCache {

        private final Map<Double, Double> cachedCalculations;

        public DistanceCache() {
            this.cachedCalculations = new HashMap<>();
        }

        public double distance(@NotNull final Vector3 start, @NotNull final Vector3 end) {
            final double preVal = square(start.x - end.x) + square(start.y - end.y) + square(start.z - end.z);
            return cachedCalculations.computeIfAbsent(preVal, Math::sqrt);
        }

        private double square(final double num) {
            return num * num;
        }
    }
}
