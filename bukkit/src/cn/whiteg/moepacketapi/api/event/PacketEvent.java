package cn.whiteg.moepacketapi.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.AuthorNagException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.logging.Level;

public class PacketEvent extends Event implements Cancellable {
    final private static HandlerList handlers = new HandlerList();
    private Object packet;
    private Player player;
    private boolean cancelled;

    public PacketEvent(final Object packet,final Player p) {
        super(true);
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
        return null;
    }

    public boolean callEvent() {
        RegisteredListener[] listeners = handlers.getRegisteredListeners();
        RegisteredListener[] var4 = listeners;
        int var5 = listeners.length;
        for (int var6 = 0; var6 < var5; ++var6) {
            RegisteredListener registration = var4[var6];
            if (registration.getPlugin().isEnabled()){
                try{
                    registration.callEvent(this);
                }catch (AuthorNagException var10){
                    Plugin plugin = registration.getPlugin();
                    if (plugin.isNaggable()){
                        plugin.setNaggable(false);
                        plugin.getLogger().log(Level.SEVERE,String.format("Nag author(s): '%s' of '%s' about the following: %s",plugin.getDescription().getAuthors(),plugin.getDescription().getFullName(),var10.getMessage()));
                    }
                }catch (Throwable t){
//                    String msg = "Could not pass event " + getEventName() + " to " + registration.getPlugin().getDescription().getFullName();
//                    MoePacketAPI.getInstance().getLogger().log(Level.SEVERE,msg,var11);
//                    if (!(event instanceof ServerExceptionEvent)){
//                        this.callEvent(new ServerExceptionEvent(new ServerEventException(msg,var11,registration.getPlugin(),registration.getListener(),this)));
//                    }
                    t.printStackTrace();
                }
            }
        }
        return !isCancelled();
    }
}
