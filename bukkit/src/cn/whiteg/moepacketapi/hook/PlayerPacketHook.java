package cn.whiteg.moepacketapi.hook;

import net.minecraft.server.v1_16_R1.NetworkManager;
import org.bukkit.entity.Player;

public interface PlayerPacketHook {
    Player getPlayer();

    void setPlayer(Player player);

    NetworkManager getNetworkManager();

    void setNetworkManager(NetworkManager networkManager);
}
