package cn.whiteg.moepacketapi.api.packet;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class PacketEvent implements Cancellable {
    private Object packet;
    private Player player;
    private boolean cancelled;

    public PacketEvent(final Object packet,final Player p) {
        this.packet = packet;
        this.player = p;
    }

    public String getName() {
        return this.packet.getClass().getSimpleName();
    }

    public Object getPacket() {
        return this.packet;
    }

    public Player getPlayer() {
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
}
