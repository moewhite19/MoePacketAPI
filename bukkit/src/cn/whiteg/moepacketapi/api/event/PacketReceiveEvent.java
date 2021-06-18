package cn.whiteg.moepacketapi.api.event;

import cn.whiteg.moepacketapi.hook.PlayerPacketHook;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.protocol.Packet;

public class PacketReceiveEvent extends PacketEvent {
    public PacketReceiveEvent(final Packet<?> packet,ChannelHandlerContext channel,PlayerPacketHook packetHook) {
        super(packet,channel,packetHook);
    }
}
