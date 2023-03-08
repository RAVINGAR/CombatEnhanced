package com.ravingarinc.eldenrhym.file;

import com.ravingarinc.eldenrhym.EldenRhym;
import com.ravingarinc.eldenrhym.api.Module;
import com.ravingarinc.eldenrhym.combat.CombatManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

public class ConfigManager extends Module {
    private final ConfigFile configFile;

    public ConfigManager(final EldenRhym plugin) {
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
            EldenRhym.log(Level.WARNING, "config.yml was invalid! Please regenerate defaults!");
            return;
        }

        final CombatManager manager = plugin.getModule(CombatManager.class);
        final Settings settings = manager.getSettings();

        wrap(() -> section.getBoolean("debug", false)).ifPresent(b -> EldenRhym.debug = b);
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
            EldenRhym.log(Level.WARNING, "config.yml is missing parry section");
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
            EldenRhym.log(Level.WARNING, "config.yml is missing block section");
        }
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
