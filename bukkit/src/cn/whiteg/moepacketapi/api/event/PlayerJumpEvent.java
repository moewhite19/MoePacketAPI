package cn.whiteg.moepacketapi.api.event;

import org.bukkit.event.*;
import org.bukkit.entity.*;

public class PlayerJumpEvent extends Event implements Cancellable
{
    private static HandlerList handlers;
    private boolean cancel;
    private Player player;
    
    public PlayerJumpEvent(final Player player) {
        this.cancel = false;
        this.player = player;
    }
    
    public boolean isCancelled() {
        return this.cancel;
    }
    
    public void setCancelled(final boolean b) {
        this.cancel = b;
    }
    
    public Player getPlayer() {
        return this.player;
    }
    
    public HandlerList getHandlers() {
        return PlayerJumpEvent.handlers;
    }
    
    public static HandlerList getHandlerList() {
        return PlayerJumpEvent.handlers;
    }
    
    static {
        PlayerJumpEvent.handlers = new HandlerList();
    }
}
