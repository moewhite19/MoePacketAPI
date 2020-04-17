package cn.whiteg.moepacketapi.api.event;

import io.netty.channel.Channel;
import org.bukkit.entity.Player;

public class PacketReceiveEvent extends PacketEvent {
    public PacketReceiveEvent(final Object packet,Channel channel,final Player p) {
        super(packet,channel,p);
    }
}
