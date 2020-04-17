package cn.whiteg.moepacketapi;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class Setting {
    public final int VER = 1;
    private final MoePacketAPI plugin;
    public boolean DEBUG;
    public FileConfiguration config;

    public Setting(MoePacketAPI plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(),"config.yml");
        config = YamlConfiguration.loadConfiguration(file);
        if (config.getInt("ver") != VER){
            plugin.getLogger().info("更新配置文件");
            plugin.saveResource("config.yml",true);
            config.set("ver",VER);
            final FileConfiguration newcon = YamlConfiguration.loadConfiguration(file);
            Set<String> keys = newcon.getKeys(true);
            for (String k : keys) {
                if (config.isSet(k)) continue;
                config.set(k,newcon.get(k));
                plugin.getLogger().info("在配置文件新增值: " + k);
            }
            try{
                config.save(file);
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        DEBUG = config.getBoolean("debug");
    }
}
