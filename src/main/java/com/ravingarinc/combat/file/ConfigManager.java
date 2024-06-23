package com.ravingarinc.combat.file;

import com.ravingarinc.combat.CombatEnhanced;
import com.ravingarinc.combat.api.Module;
import com.ravingarinc.combat.combat.CombatManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

public class ConfigManager extends Module {
    private final ConfigFile configFile;

    public ConfigManager(final CombatEnhanced plugin) {
        super(ConfigManager.class, plugin);
        this.configFile = new ConfigFile(plugin, "config.yml");
    }

    @Override
    protected void load() {
        loadCombatSettings();
    }

    private void loadCombatSettings() {
        final ConfigurationSection section = configFile.getConfig().getConfigurationSection("combat-features");
        if (section == null) {
            CombatEnhanced.log(Level.WARNING, "config.yml was invalid! Please regenerate defaults!");
            return;
        }

        final CombatManager manager = plugin.getModule(CombatManager.class);
        final Settings settings = manager.getSettings();

        wrap(() -> section.getBoolean("debug", false)).ifPresent(b -> CombatEnhanced.debug = b);
        wrap(() -> section.getLong("global-cooldown")).ifPresent(b -> settings.globalCooldown = b);

        if (consumeSection(section, "dodge", (child) -> {
            wrap(() -> child.getBoolean("enabled", false)).ifPresent(b -> settings.dodgeEnabled = b);
            wrap(() -> child.getInt("particle-count")).ifPresent(b -> settings.dodgeParticleCount = b);
            wrap(() -> child.getLong("warmup")).ifPresent(b -> settings.dodgeWarmup = b);
            wrap(() -> child.getLong("duration")).ifPresent(b -> settings.dodgeDuration = b);
            wrap(() -> child.getInt("stamina-cost")).ifPresent(b -> settings.dodgeStaminaCost = b);
            wrap(() -> child.getDouble("strength")).ifPresent(b -> settings.dodgeStrength = Float.parseFloat("" + b));
            wrap(() -> child.getDouble("mitigation")).ifPresent(b -> settings.dodgeMitigation = b);
            wrap(() -> child.getStringList("applicable-damage-causes")).ifPresent(list ->
                    list.forEach(name -> convertDamageCause(name).ifPresent(cause -> settings.dodgeDamageCauses.add(cause))));
        })) {
            CombatEnhanced.log(Level.WARNING, "config.yml is missing `parry` section");
        }

        if (consumeSection(section, "block", (child) -> {
            wrap(() -> child.getBoolean("enabled", false)).ifPresent(b -> settings.blockEnabled = b);
            wrap(() -> child.getLong("duration")).ifPresent(b -> settings.blockDuration = b);
            wrap(() -> child.getDouble("success-mitigation")).ifPresent(b -> settings.blockSuccessMitigation = b);
            wrap(() -> child.getInt("success-stamina-cost")).ifPresent(b -> settings.successBlockCost = b);
            wrap(() -> child.getDouble("fail-mitigation")).ifPresent(b -> settings.blockFailMitigation = b);
            wrap(() -> child.getInt("fail-stamina-cost")).ifPresent(b -> settings.failBlockCost = b);
            wrap(() -> child.getLong("cooldown")).ifPresent(b -> settings.blockCooldown = b);
            wrap(() -> child.getDouble("throw-strength")).ifPresent(b -> settings.blockThrowStrength = Float.parseFloat("" + b));
            wrap(() -> child.getStringList("applicable-damage-causes")).ifPresent(list ->
                    list.forEach(name -> convertDamageCause(name).ifPresent(cause -> settings.blockDamageCauses.add(cause))));
        })) {
            CombatEnhanced.log(Level.WARNING, "config.yml is missing `block` section");
        }

        if(consumeSection(section, "poise", (child) -> {
            wrap(() -> child.getLong("window")).ifPresent(b -> settings.poiseWindow = b);
            wrap(() -> child.getDouble("mob-default-poise")).ifPresent(b -> settings.mobDefaultPoise = b);
            wrap(() -> child.getDouble("stun-threshold")).ifPresent(b -> settings.stunThreshold = b.floatValue());
            wrap(() -> child.getLong("stun-duration-per-threshold")).ifPresent(b -> settings.stunDurationPerThreshold = b);
            wrap(() -> child.getLong("min-stun-duration")).ifPresent(b -> settings.minStunDuration = b);
            wrap(() -> child.getLong("max-stun-duration")).ifPresent(b -> settings.maxStunDuration = b);
            wrap(() -> child.getLong("stun-cooldown")).ifPresent(b -> settings.stunCooldown = b);
            wrap(() -> child.getDouble("vulnerability-per-poise")).ifPresent(b -> settings.vulnerabilityPerPoise = b);
            wrap(() -> child.getDouble("bonus-vulnerability")).ifPresent(b -> settings.bonusVulnerability = b);
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
    protected void reload() {
        this.configFile.reloadConfig();
    }

    @Override
    protected void shutdown() {

    }
}
