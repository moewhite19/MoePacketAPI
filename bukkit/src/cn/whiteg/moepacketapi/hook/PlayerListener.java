package cn.whiteg.moepacketapi.hook;

import cn.whiteg.moepacketapi.MoePacketAPI;
import cn.whiteg.moepacketapi.api.event.PacketReceiveEvent;
import cn.whiteg.moepacketapi.api.event.PacketSendEvent;
import cn.whiteg.moepacketapi.utils.NMSUtils;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PlayerListener implements Listener,IHook{
    final private MoePacketAPI plugin;
    final private Executor executor;

    public PlayerListener(MoePacketAPI plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadExecutor((new ThreadFactoryBuilder()).setDaemon(true).setNameFormat("MoePacketAPI Async Channel Builder - #%d").build());
        for (Player player : Bukkit.getOnlinePlayers()) {
            addChannel(player);
            player.sendMessage("加载插件");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(final PlayerJoinEvent e) {
        this.addChannel(e.getPlayer());
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent e) {
        this.removeChannel(e.getPlayer());
    }

    private void addChannel(final Player p) {
        final Channel channel = NMSUtils.getChannel(p);
        this.executor.execute(() -> {
            if (channel != null){
                if (channel.pipeline().get(plugin.getName()) != null){
                    channel.pipeline().remove(plugin.getName());
                }
                channel.pipeline().addBefore("packet_handler",plugin.getName(),new PlayerChannel(p,channel));
            }
        });
    }

    private void removeChannel(final Player p) {
        final Channel channel = NMSUtils.getChannel(p);
        this.executor.execute(() -> {
            if (channel != null && channel.pipeline().get(plugin.getName()) != null){
                channel.pipeline().remove(plugin.getName());
            }
        });
    }

    @Override
    public void close() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Channel c = NMSUtils.getChannel(player);
            c.pipeline().remove(plugin.getName());
        }
    }
}
