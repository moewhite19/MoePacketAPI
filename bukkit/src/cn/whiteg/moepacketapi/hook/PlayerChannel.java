package cn.whiteg.moepacketapi.hook;

import cn.whiteg.moepacketapi.api.event.PacketReceiveEvent;
import cn.whiteg.moepacketapi.api.event.PacketSendEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.bukkit.entity.Player;

public class PlayerChannel extends ChannelDuplexHandler {
    private volatile Player player;
    private Channel channel;

    public PlayerChannel(Player p,Channel channel) {
        this.player = p;
        this.channel = channel;
    }

    @Override
    public void write(final ChannelHandlerContext ctx,final Object packet,final ChannelPromise promise) throws Exception {
        PacketSendEvent e = new PacketSendEvent(packet,channel,player);
        if (e.callEvent()){
            super.write(ctx,e.getPacket(),promise);
        }
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx,final Object packet) throws Exception {
        PacketReceiveEvent e = new PacketReceiveEvent(packet,channel,player);
        if (e.callEvent()){
            super.channelRead(ctx,e.getPacket());
        }
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}
