package cn.whiteg.moepacketapi;

import cn.whiteg.moepacketapi.api.packet.PacketEvent;
import cn.whiteg.moepacketapi.api.packet.PacketHandler;
import cn.whiteg.moepacketapi.utils.Reflection;
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

public class PlayerListener implements Listener {
    final private MoePacketAPI plugin;
    final private Executor executor;

    public PlayerListener(MoePacketAPI plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadExecutor((new ThreadFactoryBuilder()).setDaemon(true).setNameFormat("MoePacketAPI Async Channel Builder - #%d").build());

        Bukkit.getPluginManager().registerEvents(this,plugin);
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
        final Channel channel = Reflection.getChannel(p);
        this.executor.execute(() -> {
            if (channel != null){
                if (channel.pipeline().get(plugin.getName()) != null){
                    channel.pipeline().remove(plugin.getName());
                }
                channel.pipeline().addBefore("packet_handler",plugin.getName(),new ChannelListener(p));
            }
        });
    }

    private void removeChannel(final Player p) {
        final Channel channel = Reflection.getChannel(p);
        this.executor.execute(() -> {
            if (channel != null && channel.pipeline().get(plugin.getName()) != null){
                channel.pipeline().remove(plugin.getName());
            }
        });
    }

    private final class ChannelListener extends ChannelDuplexHandler {
        private Player p;

        public ChannelListener(final Player p) {
            this.p = p;
        }

        @Override
        public void write(final ChannelHandlerContext ctx,final Object packet,final ChannelPromise promise) throws Exception {

//            if (PacketRefactor.getRefactor().refact(packet,this.p,ctx)){
//                return;
//            }
            PacketEvent e = new PacketEvent(packet,this.p);
            for (final PacketHandler handler : plugin.getPacketManager().getPacketHandles()) {
                try{
                    handler.onPacketSending(e);
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
            if (e.isCancelled()) return;
            super.write(ctx,packet,promise);
        }

        @Override
        public void channelRead(final ChannelHandlerContext ctx,final Object packet) throws Exception {
//            if (PacketRefactor.getRefactor().refact(packet,this.p,ctx)){
//                return;
//            }
            PacketEvent e = new PacketEvent(packet,this.p);
            for (final PacketHandler handler : plugin.getPacketManager().getPacketHandles()) {
                try{
                    handler.onPacketReceiving(e);
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
            if (e.isCancelled()) return;
            super.channelRead(ctx,packet);
        }
    }
}
