package cn.whiteg.moepacketapi.api.event;

import cn.whiteg.moepacketapi.hook.PlayerPacketHook;
import io.netty.channel.ChannelHandlerContext;

public class PacketSendEvent extends PacketEvent {

    public PacketSendEvent(final Object packet,ChannelHandlerContext channel,PlayerPacketHook packetHook) {
        super(packet,channel,packetHook);
    }
}
