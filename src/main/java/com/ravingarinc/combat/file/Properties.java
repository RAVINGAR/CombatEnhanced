package com.ravingarinc.combat.file;

import com.ravingarinc.api.module.Module;
import com.ravingarinc.api.module.RavinPlugin;
import com.ravingarinc.combat.CombatEnhanced;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

public class Properties extends Module {
    private final ConfigFile configFile;

    public long globalCooldown = 50;
    public boolean dodgeEnabled = true;
    public long dodgeWarmup = 100;
    public long dodgeDuration = 300;
    public float dodgeStrength = 0.5F;

    public int dodgeStaminaCost = 50;
    public double dodgeMitigation = 0.5;
    public int dodgeParticleCount = 5;

    public List<DamageCause> dodgeDamageCauses = new ArrayList<>();

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

    public Properties(final RavinPlugin plugin) {
        super(Properties.class, plugin);
        this.configFile = new ConfigFile(plugin, "config.yml");
    }

    @Override
    public void load() {
        loadCombatSettings();
    }

    private void loadCombatSettings() {
        final ConfigurationSection section = configFile.getConfig().getConfigurationSection("combat-features");
        if (section == null) {
            CombatEnhanced.log(Level.WARNING, "config.yml was invalid! Please regenerate defaults!");
            return;
        }
        wrap(() -> section.getLong("global-cooldown")).ifPresent(b -> globalCooldown = b);

        if (consumeSection(section, "dodge", (child) -> {
            wrap(() -> child.getBoolean("enabled", false)).ifPresent(b -> dodgeEnabled = b);
            wrap(() -> child.getInt("particle-count")).ifPresent(b -> dodgeParticleCount = b);
            wrap(() -> child.getLong("warmup")).ifPresent(b -> dodgeWarmup = b);
            wrap(() -> child.getLong("duration")).ifPresent(b -> dodgeDuration = b);
            wrap(() -> child.getInt("stamina-cost")).ifPresent(b -> dodgeStaminaCost = b);
            wrap(() -> child.getDouble("strength")).ifPresent(b -> dodgeStrength = Float.parseFloat("" + b));
            wrap(() -> child.getDouble("mitigation")).ifPresent(b -> dodgeMitigation = b);
            wrap(() -> child.getStringList("applicable-damage-causes")).ifPresent(list ->
                    list.forEach(name -> convertDamageCause(name).ifPresent(cause -> dodgeDamageCauses.add(cause))));
        })) {
            CombatEnhanced.log(Level.WARNING, "config.yml is missing `parry` section");
        }

        if (consumeSection(section, "block", (child) -> {
            wrap(() -> child.getBoolean("enabled", false)).ifPresent(b -> blockEnabled = b);
            wrap(() -> child.getLong("duration")).ifPresent(b -> blockDuration = b);
            wrap(() -> child.getDouble("success-mitigation")).ifPresent(b -> blockSuccessMitigation = b);
            wrap(() -> child.getInt("success-stamina-cost")).ifPresent(b -> successBlockCost = b);
            wrap(() -> child.getDouble("fail-mitigation")).ifPresent(b -> blockFailMitigation = b);
            wrap(() -> child.getInt("fail-stamina-cost")).ifPresent(b -> failBlockCost = b);
            wrap(() -> child.getLong("cooldown")).ifPresent(b -> blockCooldown = b);
            wrap(() -> child.getDouble("throw-strength")).ifPresent(b -> blockThrowStrength = Float.parseFloat("" + b));
            wrap(() -> child.getStringList("applicable-damage-causes")).ifPresent(list ->
                    list.forEach(name -> convertDamageCause(name).ifPresent(cause -> blockDamageCauses.add(cause))));
        })) {
            CombatEnhanced.log(Level.WARNING, "config.yml is missing `block` section");
        }

        if(consumeSection(section, "poise", (child) -> {
            wrap(() -> child.getLong("window")).ifPresent(b -> poiseWindow = b);
            wrap(() -> child.getDouble("mob-default-poise")).ifPresent(b -> mobDefaultPoise = b);
            wrap(() -> child.getDouble("stun-threshold")).ifPresent(b -> stunThreshold = b.floatValue());
            wrap(() -> child.getLong("stun-duration-per-threshold")).ifPresent(b -> stunDurationPerThreshold = b);
            wrap(() -> child.getLong("min-stun-duration")).ifPresent(b -> minStunDuration = b);
            wrap(() -> child.getLong("max-stun-duration")).ifPresent(b -> maxStunDuration = b);
            wrap(() -> child.getLong("stun-cooldown")).ifPresent(b -> stunCooldown = b);
            wrap(() -> child.getDouble("vulnerability-per-poise")).ifPresent(b -> vulnerabilityPerPoise = b);
            wrap(() -> child.getDouble("bonus-vulnerability")).ifPresent(b -> bonusVulnerability = b);
        })) {
            CombatEnhanced.log(Level.WARNING, "config.yml is missing `poise` section");
        };
    }

    private Optional<DamageCause> convertDamageCause(final String name) {
        for (final DamageCause cause : DamageCause.values()) {
            if (cause.name().equalsIgnoreCase(name)) {
                return Optional.of(cause);
            }
        }
        return Optional.empty();
    }

    /**
     * Validates if a configuration section exists at the path from parent. If it does exist then it is consumed
     *
     * @param parent   The parent section
     * @param path     The path to child section
     * @param consumer The consumer
     * @return true if section was invalid
     */
    private boolean consumeSection(final ConfigurationSection parent, final String path, final Consumer<ConfigurationSection> consumer) {
        final ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            return true;
        }
        consumer.accept(section);

        return false;
    }

    private <V> Optional<V> wrap(final Supplier<V> supplier) {
        return Optional.ofNullable(supplier.get());
    }

    @Override
    public void cancel() {
        this.configFile.reloadConfig();
    }
}
