package cn.whiteg.moepacketapi;

import cn.whiteg.moepacketapi.utils.Reflection;
import io.netty.channel.Channel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class MoePacketAPI extends JavaPlugin {
    private static MoePacketAPI instance;
    private PlayerPacketManage playerPacketManage = new PlayerPacketManage();

    public MoePacketAPI() {
        MoePacketAPI.instance = this;
    }

    public static MoePacketAPI getInstance() {
        return MoePacketAPI.instance;
    }

    public void onEnable() {
        new PlayerListener(this);
        this.getLogger().info(this.getName() + " 已启用");
    }

    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Channel c = Reflection.getChannel(player);
            c.pipeline().remove(getName());
//            player.sendMessage("卸载插件");
        }
    }

    public PlayerPacketManage getPlayerPacketManage() {
        return playerPacketManage;
    }
}
