package com.ravingarinc.combat.file;

import org.bukkit.event.entity.EntityDamageEvent;

import java.util.ArrayList;
import java.util.List;

public class Settings {

    public long globalCooldown = 50;
    public boolean dodgeEnabled = true;
    public long dodgeWarmup = 100;
    public long dodgeDuration = 300;
    public float dodgeStrength = 0.5F;

    public int dodgeStaminaCost = 50;
    public double dodgeMitigation = 0.5;
    public int dodgeParticleCount = 5;

    public List<EntityDamageEvent.DamageCause> dodgeDamageCauses = new ArrayList<>();

    public boolean blockEnabled = true;
    public long blockDuration = 600;

    public int successBlockCost = 50;

    public int failBlockCost = 100;

    public double blockSuccessMitigation = 1.0;

    public double blockFailMitigation = 0.5;
    public long blockCooldown = 500;

    public float blockThrowStrength = 0.3f;

    public List<EntityDamageEvent.DamageCause> blockDamageCauses = new ArrayList<>();

    public long poiseWindow = 2000L;

    public double mobDefaultPoise = 30.0;

    public double stunThreshold = 5f;

    public long stunDurationPerThreshold = 500L;

    public long minStunDuration = 1500L;

    public long maxStunDuration = 3000L;

    public long stunCooldown = 1000L;

    public double vulnerabilityPerPoise = 0.02;
    public double bonusVulnerability = 0.005;
}
