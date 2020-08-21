package cn.whiteg.moepacketapi.api.event;

import cn.whiteg.moepacketapi.hook.PlayerPacketHook;
import io.netty.channel.ChannelHandlerContext;

public class PacketReceiveEvent extends PacketEvent {
    public PacketReceiveEvent(final Object packet,ChannelHandlerContext channel,PlayerPacketHook packetHook) {
        super(packet,channel,packetHook);
    }
}
