package cn.whiteg.moepacketapi;

import net.minecraft.server.v1_15_R1.Packet;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class PlayerPacketManage {
    List<Integer> caches = Collections.synchronizedList(new LinkedList<>());

    //向玩家发包
    public void sendPacket(Player player,Object packet) {
        if (player.isOnline() && packet instanceof Packet){
            setPluginPacket(packet);
            ((CraftPlayer) player).getHandle().playerConnection.networkManager.sendPacket((Packet<?>) packet);
        }
    }

    //是否为插件发包
    public boolean isPluginPacket(Object packet) {
        return caches.contains(packet.hashCode());
    }


    //设置插件发包
    public void setPluginPacket(Object packet) {
        caches.add(packet.hashCode());
        if (caches.size() > 4) caches.remove(0);
    }
}
