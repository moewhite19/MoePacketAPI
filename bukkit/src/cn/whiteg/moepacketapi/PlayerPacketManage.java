package cn.whiteg.moepacketapi;

import cn.whiteg.moepacketapi.utils.EntityNetUtils;
import cn.whiteg.moepacketapi.utils.FieldAccessor;
import cn.whiteg.moepacketapi.utils.ReflectionUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class PlayerPacketManage {
    private static FieldAccessor<PacketListener> newtworkPacketListener;
    private static FieldAccessor<Channel> networkChannel;
    private static FieldAccessor<ServerPlayer> connectionEntityPlayer;

    static {
        try{
            newtworkPacketListener = ReflectionUtils.getFieldFormType(Connection.class,PacketListener.class);
            networkChannel = ReflectionUtils.getFieldFormType(Connection.class,Channel.class);
            connectionEntityPlayer = ReflectionUtils.getFieldFormType(ServerGamePacketListenerImpl.class,ServerPlayer.class);
        }catch (Exception e){
            e.printStackTrace();
        }
//            networkManageRead = Connection.class.getDeclaredMethod("channelRead0",ChannelHandlerContext.class,Packet.class);
//            networkManageRead.setAccessible(true);
//            packetListener = Connection.class.getDeclaredField("m");
    }

    private final Set<Integer> cache = Collections.newSetFromMap(new WeakHashMap<>());

    PlayerPacketManage() {
    }

    //向玩家发包
    public void sendPacket(Player player,Packet<?> packet) {
        if (player.isOnline() && packet != null){
            setPluginPacket(packet);
            getNetworkManage(player).send(packet);
        }
    }

    //是否为插件发包
    public boolean isPluginPacket(Packet<?> packet) {
        synchronized (cache) {
            return cache.contains(packet.hashCode());
        }
    }


    //设置插件发包
    public void setPluginPacket(Packet<?> packet) {
        synchronized (cache) {
            cache.add(packet.hashCode());
        }
    }

    //模拟服务端收包
    public void recieveClientPacket(Channel channel,Packet<?> packet) {
        Connection network = getNetworkManage(channel);
        if (network == null) return;
        recieveClientPacket(network,(packet));
    }

    public void recieveClientPacket(Connection networkManager,Packet<?> packet) {
        if (newtworkPacketListener.get(networkManager) instanceof ServerGamePacketListener listenerPlayIn && packet != null){
            recieveClientPacket(listenerPlayIn,((Packet<ServerGamePacketListener>) packet));
        } else {
            recieveClientPacket(networkManager,getChannel(networkManager).pipeline().lastContext(),packet);
        }
    }

    public void recieveClientPacket(Connection networkManager,ChannelHandlerContext ctx,Packet<?> packet) {
        if (!getChannel(networkManager).isOpen()) return;
        try{
            networkManager.channelRead(ctx,packet);
        }catch (Exception e){
            throw new IllegalStateException("发包错误");
        }
    }

    //服务端收包事件
    public void recieveClientPacket(ServerGamePacketListener networkManager,Packet<ServerGamePacketListener> packet) {
        packet.handle(networkManager);
    }

    public Connection getNetworkManage(Channel channel) {
        ChannelHandler h = channel.pipeline().get("packet_handler");
        if (h instanceof Connection) return (Connection) h;
        return null;
    }

    public Connection getNetworkManage(Player player) {
        return EntityNetUtils.getNetWork(EntityNetUtils.getPlayerConnection(EntityNetUtils.getNmsPlayer(player)));
    }

    public ServerGamePacketListenerImpl getServerGamePacketListenerImpl(Player player) {
        return EntityNetUtils.getPlayerConnection(EntityNetUtils.getNmsPlayer(player));
    }


    public Player getPlayer(Channel channel) {
        return getPlayer(getNetworkManage(channel));
    }

    public Player getPlayer(Connection network) {
        if (network == null) return null;
        PacketListener listener = newtworkPacketListener.get(network);
        if (listener instanceof ServerGamePacketListenerImpl playerConnection){
//            return connectionEntityPlayer.get(playerConnection).getBukkitEntity();
            return playerConnection.getCraftPlayer();
        }
        return null;
    }

    public Channel getChannel(Player player) {
        return getChannel(getNetworkManage(player));
    }

    public Channel getChannel(Connection networkManager) {
        return networkChannel.get(networkManager);
    }

    public Set<Integer> getCache() {
        return cache;
    }
}
