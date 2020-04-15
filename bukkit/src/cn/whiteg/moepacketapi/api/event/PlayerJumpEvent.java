package cn.whiteg.moepacketapi.api.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerJumpEvent extends Event implements Cancellable {

    final private static HandlerList handlers = new HandlerList();
    private boolean cancel;
    private Player player;

    public PlayerJumpEvent(final Player player) {
        this.cancel = false;
        this.player = player;
    }

    public static HandlerList getHandlerList() {
        return PlayerJumpEvent.handlers;
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

    @Override
    public boolean callEvent() {
        Bukkit.getPluginManager().callEvent(this);
        return !isCancelled();
    }
}
