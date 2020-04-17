package cn.whiteg.moepacketapi.api.event;

import cn.whiteg.moepacketapi.MoePacketAPI;
import io.netty.channel.Channel;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class PacketReceiveEvent extends PacketEvent {
    final private static HandlerList handlers = new HandlerList();

    public PacketReceiveEvent(final Object packet,Channel channel,final Player p) {
        super(packet,channel,p);
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public boolean isPluginPacket() {
        return MoePacketAPI.getInstance().getPlayerPacketManage().isPluginPacket(this);
    }
}
