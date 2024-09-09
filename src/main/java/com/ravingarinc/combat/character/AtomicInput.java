package com.ravingarinc.combat.character;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

public class AtomicInput {
    private final LinkedBlockingDeque<Direction> lastInputs = new LinkedBlockingDeque<>(8);
    private final AtomicLong lastUpdateTime = new AtomicLong(System.currentTimeMillis());
    public AtomicInput() {

    }

    public boolean update(double forwards, double sideways) {
        final var time = System.currentTimeMillis();
        final var lastTime = lastUpdateTime.get();
        if(lastTime + 100 > time) {
            return false;
        }

        lastUpdateTime.set(time);
        if(lastInputs.size() == 8) {
            lastInputs.poll();
        }
        lastInputs.offer(new Direction(forwards, sideways, time));
        return movementCheck();
    }

    public boolean movementCheck() {
        if(lastInputs.size() != 8) return false;
        var iterator = lastInputs.iterator();
        var lastDir = iterator.next();
        var xAvg = 0.0;
        var zAvg = 0.0;
        var amount = 0;
        double lastX = 0.0, lastZ = 0.0;
        while(iterator.hasNext()) {
            final var dir = iterator.next();
            final var xDiff =(dir.x - lastDir.x);
            final var zDiff = (dir.z - lastDir.z);
            xAvg += xDiff;
            zAvg += zDiff;
            lastDir = dir;
            if(!iterator.hasNext()) {
                lastX = xDiff;
                lastZ = zDiff;
            }
            amount++;
        }
        xAvg /= amount;
        zAvg /= amount;

        var total = Math.abs(lastX - xAvg) + Math.abs(lastZ - zAvg);

        return total > 0.8;
    }

    private record Direction(double x, double z, long time) {};
}
