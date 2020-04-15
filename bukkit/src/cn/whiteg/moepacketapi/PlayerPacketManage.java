package cn.whiteg.moepacketapi;

import net.minecraft.server.v1_15_R1.Packet;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class PlayerPacketManage {
    List<Integer> caches = Collections.synchronizedList(new LinkedList<>());

    public void sendPacket(Player player,Object packet) {
        if (player.isOnline() && packet instanceof Packet){
            ((CraftPlayer) player).getHandle().playerConnection.networkManager.sendPacket((Packet<?>) packet);
            setPluginPacket(packet);
        }
    }

    public boolean isPluginPacket(Object packet) {
        return caches.contains(packet.hashCode());
    }

    public void setPluginPacket(Object packet) {
        caches.add(packet.hashCode());
        if (caches.size() > 4) caches.remove(0);
    }
}
