package cn.whiteg.moepacketapi.api.event;

import cn.whiteg.moepacketapi.MoePacketAPI;
import io.netty.channel.Channel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class PacketEvent extends Event implements Cancellable {
    final private static HandlerList handlers = new HandlerList();
    private final Channel channel;
    private Object packet;
    private Player player;
    private boolean cancelled;


    public PacketEvent(final Object packet,Channel channel,Player p) {
        super(true);
        this.channel = channel;
        this.packet = packet;
        this.player = p;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public String getName() {
        return this.packet.getClass().getSimpleName();
    }

    public Object getPacket() {
        return this.packet;
    }

    public void setPacket(Object packet) {
        this.packet = packet;
    }

    public Player getPlayer() {
        if (MoePacketAPI.getInstance().getSetting().DEBUG && player == null)
            MoePacketAPI.getInstance().getLogger().warning("Player值为 Null");
        return this.player;
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
        return channel;
    }

    public boolean isPluginPacket() {
        return MoePacketAPI.getInstance().getPlayerPacketManage().isPluginPacket(this);
    }
}
