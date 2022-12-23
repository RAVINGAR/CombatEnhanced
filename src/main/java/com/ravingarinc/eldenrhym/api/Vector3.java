package com.ravingarinc.eldenrhym.api;

import org.bukkit.util.Vector;

import javax.annotation.concurrent.ThreadSafe;
import javax.vecmath.Vector3d;

/**
 * Vector safe to use in asynchronous applications. Also has useful methods.
 */
@ThreadSafe
public class Vector3 extends Vector3d {

    public Vector3() {
        super();
    }

    public Vector3(final double x, final double y, final double z) {
        super(x, y, z);
    }

    /**
     * Create a Bukkit Vector object.
     *
     * @return vector
     */
    public Vector toBukkitVector() {
        return new Vector(this.getX(), this.getY(), this.getZ());
    }
}
