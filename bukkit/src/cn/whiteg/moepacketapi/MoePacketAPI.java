package cn.whiteg.moepacketapi;

import cn.whiteg.moepacketapi.hook.IHook;
import cn.whiteg.moepacketapi.hook.PlayerListener;
import cn.whiteg.moepacketapi.hook.TinyProtocolHook;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class MoePacketAPI extends JavaPlugin {
    private static MoePacketAPI instance;
    private PlayerPacketManage playerPacketManage = new PlayerPacketManage();
    private IHook hook;
    private Setting setting;

    public MoePacketAPI() {
        MoePacketAPI.instance = this;
    }

    public static MoePacketAPI getInstance() {
        return MoePacketAPI.instance;
    }

    @Override
    public void onLoad() {
        saveDefaultConfig();
    }

    public void onEnable() {
//        new PlayerListener(this);
        setting = new Setting(this);
        try{
            hook = new TinyProtocolHook(this);
        }catch (Exception e){
            getLogger().warning("出现异常使用,兼容模式");
            PlayerListener playerListener = new PlayerListener(this);
            Bukkit.getPluginManager().registerEvents(playerListener,this);
        }
        this.getLogger().info(this.getName() + " 已启用");
    }

    public void onDisable() {
        if (hook != null){
            if (hook instanceof Listener) HandlerList.unregisterAll((Listener) hook);
            hook.close();
        }
    }

    public Setting getSetting() {
        return setting;
    }

    public PlayerPacketManage getPlayerPacketManage() {
        return playerPacketManage;
    }
}
