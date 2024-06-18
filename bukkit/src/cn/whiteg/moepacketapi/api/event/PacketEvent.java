package cn.whiteg.moepacketapi.api.event;

import cn.whiteg.moepacketapi.MoePacketAPI;
import cn.whiteg.moepacketapi.hook.PlayerPacketHook;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class PacketEvent extends Event implements Cancellable {
    final private static HandlerList handlers = new HandlerList();
    private final ChannelHandlerContext channel;
    private final PlayerPacketHook handel;
    private Packet<?> packet;
    private boolean cancelled;


    public PacketEvent(final Packet<?> packet,ChannelHandlerContext channel,PlayerPacketHook p) {
        super(true);
        this.channel = channel;
        this.packet = packet;
        this.handel = p;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public String getName() {
        return this.packet.getClass().getSimpleName();
    }

    public Packet<?> getPacket() {
        return this.packet;
    }

    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }

    public Player getPlayer() {
        Player player = handel.getPlayer();
        if (player == null){
            player = MoePacketAPI.getInstance().getPlayerPacketManage().getPlayer(getNetworkManage());
            handel.setPlayer(player);
        }
        return player;
    }

    public Connection getNetworkManage() {
        Connection network = handel.getConnection();
        if (network == null){
            network = MoePacketAPI.getInstance().getPlayerPacketManage().getNetworkManage(getChannel());
            handel.setConnection(network);
        }
        return network;
    }

    public Object getField(final String name) {
        Object value = null;
        try{
            final Field f = this.isSuper() ? this.packet.getClass().getSuperclass().getDeclaredField(name) : this.packet.getClass().getDeclaredField(name);
            f.setAccessible(true);
            value = f.get(this.packet);
        }catch (Exception e){
            e.printStackTrace();
        }
        return value;
    }

    private boolean isSuper() {
        return Modifier.isStatic(this.packet.getClass().getModifiers());
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        cancelled = b;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public boolean callEvent() {
        Bukkit.getPluginManager().callEvent(this);
        return !isCancelled();
    }

    public Channel getChannel() {
        return channel.channel();
    }

    public ChannelHandlerContext getChannelHandleContext() {
        return channel;
    }

    public boolean isPluginPacket() {
        return MoePacketAPI.getInstance().getPlayerPacketManage().isPluginPacket(this.getPacket());
    }
}
