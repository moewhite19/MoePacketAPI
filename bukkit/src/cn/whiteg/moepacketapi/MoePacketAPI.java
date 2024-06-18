package cn.whiteg.moepacketapi;

import cn.whiteg.moepacketapi.hook.IHook;
import cn.whiteg.moepacketapi.hook.TinyProtocol;
import org.bukkit.plugin.java.JavaPlugin;

public class MoePacketAPI extends JavaPlugin {
    private static MoePacketAPI instance;
    final private PlayerPacketManage playerPacketManage;
    private IHook hook;
    private Setting setting;

    public MoePacketAPI() {
        MoePacketAPI.instance = this;
        playerPacketManage = new PlayerPacketManage();
    }

    public static MoePacketAPI getInstance() {
        return MoePacketAPI.instance;
    }

    @Override
    public void onLoad() {
        saveDefaultConfig();
    }

    public void onEnable() {
        setting = new Setting(this);
        try{
            hook = new TinyProtocol(this);
        }catch (Exception e){
            getLogger().warning("出现异常");
            e.printStackTrace();
//            getLogger().warning("出现异常使用,兼容模式");
//            hook = new PlayerListener(this);
        }
        this.getLogger().info(this.getName() + " 已启用");
    }

    public void onDisable() {
        if (hook != null){
            hook.close();
        }
    }

    public IHook getHook() {
        return hook;
    }

    public Setting getSetting() {
        return setting;
    }

    public PlayerPacketManage getPlayerPacketManage() {
        return playerPacketManage;
    }
}
