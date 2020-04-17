package cn.whiteg.moepacketapi.hook;

import cn.whiteg.moepacketapi.MoePacketAPI;
import cn.whiteg.moepacketapi.api.event.PacketEvent;
import cn.whiteg.moepacketapi.api.event.PacketReceiveEvent;
import cn.whiteg.moepacketapi.api.event.PacketSendEvent;
import io.netty.channel.Channel;
import org.bukkit.entity.Player;

public class TinyProtocolHook extends TinyProtocol {
    public TinyProtocolHook(MoePacketAPI plugin) {
        super(plugin);
    }

    /**
     * Invoked when the server is starting to send a packet to a player.
     * <p>
     * Note that this is not executed on the main thread.
     *
     * @param receiver - the receiving player, NULL for early login/status packets.
     * @param channel  - the channel that received the packet. Never NULL.
     * @param packet   - the packet being sent.
     * @return The packet to send instead, or NULL to cancel the transmission.
     */
    @Override
    public Object onPacketOutAsync(Player receiver,Channel channel,Object packet) {
        PacketSendEvent event = new PacketSendEvent(packet,channel,receiver);
        if (event.callEvent()){
            return event.getPacket();
        }
        return null;
    }

    /**
     * Invoked when the server has received a packet from a given player.
     * <p>
     * Use {@link Channel#remoteAddress()} to get the remote address of the client.
     *
     * @param sender  - the player that sent the packet, NULL for early login/status packets.
     * @param channel - channel that received the packet. Never NULL.
     * @param packet  - the packet being received.
     * @return The packet to recieve instead, or NULL to cancel.
     */
    @Override
    public Object onPacketInAsync(Player sender,Channel channel,Object packet) {
        PacketEvent event = new PacketReceiveEvent(packet,channel,sender);
        if (event.callEvent()){
            return event.getPacket();
        }
        return null;
    }
}
