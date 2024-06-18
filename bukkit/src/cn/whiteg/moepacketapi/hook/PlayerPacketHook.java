package cn.whiteg.moepacketapi.hook;

import net.minecraft.network.Connection;
import org.bukkit.entity.Player;

public interface PlayerPacketHook {
    Player getPlayer();

    void setPlayer(Player player);

    Connection getConnection();

    void setConnection(Connection networkManager);
}
