package com.ravingarinc.eldenrhym.file;

import com.ravingarinc.eldenrhym.EldenRhym;
import com.ravingarinc.eldenrhym.api.Module;
import com.ravingarinc.eldenrhym.combat.CombatManager;
import org.bukkit.configuration.ConfigurationSection;

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
        final CombatManager.Settings settings = manager.getSettings();

        wrap(() -> section.getBoolean("debug", false)).ifPresent(b -> EldenRhym.debug = b);

        if (consumeSection(section, "counter-attack", (child) -> {
            wrap(() -> child.getBoolean("enabled", false)).ifPresent(b -> settings.counterEnabled = b);
            wrap(() -> child.getLong("start-time")).ifPresent(b -> settings.counterStartTime = b);
            wrap(() -> child.getLong("end-time")).ifPresent(b -> settings.counterEndTime = b);
        })) {
            EldenRhym.log(Level.WARNING, "config.yml is missing counter-attack section");
        }

        if (consumeSection(section, "parry", (child) -> {
            wrap(() -> child.getBoolean("enabled", false)).ifPresent(b -> settings.parryEnabled = b);
        })) {
            EldenRhym.log(Level.WARNING, "config.yml is missing parry section");
        }

        if (consumeSection(section, "dodge", (child) -> {
            wrap(() -> child.getBoolean("enabled", false)).ifPresent(b -> settings.dodgeEnabled = b);
            wrap(() -> child.getLong("warmup")).ifPresent(b -> settings.dodgeWarmup = b);
            wrap(() -> child.getLong("duration")).ifPresent(b -> settings.dodgeDuration = b);
            wrap(() -> child.getDouble("strength")).ifPresent(b -> settings.dodgeStrength = Float.parseFloat("" + b));
            wrap(() -> child.getDouble("mitigation")).ifPresent(b -> settings.dodgeMitigation = b);
        })) {
            EldenRhym.log(Level.WARNING, "config.yml is missing parry section");
        }

        if (consumeSection(section, "block", (child) -> {
            wrap(() -> child.getBoolean("enabled", false)).ifPresent(b -> settings.blockEnabled = b);
            wrap(() -> child.getLong("duration")).ifPresent(b -> settings.blockDuration = b);
            wrap(() -> child.getDouble("mitigation")).ifPresent(b -> settings.blockMitigation = b);
        })) {
            EldenRhym.log(Level.WARNING, "config.yml is missing block section");
        }
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
