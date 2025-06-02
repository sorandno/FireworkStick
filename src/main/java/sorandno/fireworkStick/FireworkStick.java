package sorandno.fireworkStick;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class FireworkStick extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("FireworkStick プラグインが開始しました");
        getCommand("getfireworkstick").setExecutor(new GetFireworkStickCommand(this));
        Bukkit.getPluginManager().registerEvents(new FireworkListener(this), this);
        registerConfig();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("FireworkStick プラグインが停止しました");
    }

    public void registerConfig() {
        File config = new File(getDataFolder(), "config.yml");
        if (!config.exists()) {
            getConfig().options().copyDefaults(true);
            saveConfig();
        }
    }
}
