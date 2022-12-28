package com.ravingarinc.eldenrhym.file;

import org.bukkit.event.entity.EntityDamageEvent;

import java.util.ArrayList;
import java.util.List;

public class Settings {

    public long globalCooldown = 50;
    public boolean counterEnabled = true;
    public long counterStartTime = 100;
    public long counterEndTime = 500;

    public boolean parryEnabled = true;

    public boolean dodgeEnabled = true;
    public long dodgeWarmup = 100;
    public long dodgeDuration = 300;
    public float dodgeStrength = 0.5F;
    public double dodgeMitigation = 0.5;

    public List<EntityDamageEvent.DamageCause> dodgeDamageCauses = new ArrayList<>();

    public boolean blockEnabled = true;
    public long blockDuration = 600;

    public double blockSuccessMitigation = 1.0;

    public double blockFailMitigation = 0.5;
    public long blockCooldown = 500;

    public float blockThrowStrength = 0.3f;

    public List<EntityDamageEvent.DamageCause> blockDamageCauses = new ArrayList<>();

}
