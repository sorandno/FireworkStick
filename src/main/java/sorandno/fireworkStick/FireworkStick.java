package sorandno.fireworkStick;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class FireworkStick extends JavaPlugin {
    private final List<ParticlePattern> patterns = new ArrayList<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        registerConfig();
        getCommand("getfireworkstick").setExecutor(new GetFireworkStickCommand(this));
        Bukkit.getPluginManager().registerEvents(new FireworkListener(this, patterns), this);

        saveResource("1.csv", false);
        // 必要に応じて他csvファイルもコピー
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        File[] csvFiles = getDataFolder().listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
        if (csvFiles != null) {
            for (File file : csvFiles) {
                ParticlePattern pattern = new ParticlePattern(this, file);
                patterns.add(pattern);
                getLogger().info("Loaded pattern: " + file.getName());
            }
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void registerConfig() {
        File config = new File(getDataFolder(), "config.yml");
        if (!config.exists()) {
            getConfig().options().copyDefaults(true);
            saveConfig();
        }
    }

    public List<ParticlePattern> getPatterns() {
        return patterns;
    }
}
