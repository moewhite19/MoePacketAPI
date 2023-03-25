package cn.whiteg.moepacketapi;

import cn.whiteg.moepacketapi.utils.EntityNetUtils;
import cn.whiteg.moepacketapi.utils.FieldAccessor;
import cn.whiteg.moepacketapi.utils.ReflectionUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketListenerPlayIn;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.PlayerConnection;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class PlayerPacketManage {
    private static FieldAccessor<PacketListener> newtworkPacketListener;
    private static FieldAccessor<Channel> networkChannel;
    private static FieldAccessor<EntityPlayer> connectionEntityPlayer;

    static {
        try{
            newtworkPacketListener = ReflectionUtils.getFieldFormType(NetworkManager.class,PacketListener.class);
            networkChannel = ReflectionUtils.getFieldFormType(NetworkManager.class,Channel.class);
            connectionEntityPlayer = ReflectionUtils.getFieldFormType(PlayerConnection.class,EntityPlayer.class);
        }catch (Exception e){
            e.printStackTrace();
        }
//            networkManageRead = NetworkManager.class.getDeclaredMethod("channelRead0",ChannelHandlerContext.class,Packet.class);
//            networkManageRead.setAccessible(true);
//            packetListener = NetworkManager.class.getDeclaredField("m");
    }

    private final Set<Integer> cache = Collections.newSetFromMap(new WeakHashMap<>());

    PlayerPacketManage() {
    }

    //向玩家发包
    public void sendPacket(Player player,Packet<?> packet) {
        if (player.isOnline() && packet != null){
            setPluginPacket(packet);
            getNetworkManage(player).a(packet);
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
        NetworkManager network = getNetworkManage(channel);
        if (network == null) return;
        recieveClientPacket(network,(packet));
    }
    
    public void recieveClientPacket(NetworkManager networkManager,Packet<?> packet) {
        if (newtworkPacketListener.get(networkManager) instanceof PacketListenerPlayIn listenerPlayIn && packet != null){
            recieveClientPacket(listenerPlayIn,((Packet<PacketListenerPlayIn>) packet));
        } else {
            recieveClientPacket(networkManager,getChannel(networkManager).pipeline().lastContext(),packet);
        }
    }

    public void recieveClientPacket(NetworkManager networkManager,ChannelHandlerContext ctx,Packet<?> packet) {
        if (!getChannel(networkManager).isOpen()) return;
        try{
            networkManager.channelRead(ctx,packet);
        }catch (Exception e){
            throw new IllegalStateException("发包错误");
        }
    }

    //服务端收包事件
    public void recieveClientPacket(PacketListenerPlayIn networkManager,Packet<PacketListenerPlayIn> packet) {
        packet.a(networkManager);
    }

    public NetworkManager getNetworkManage(Channel channel) {
        ChannelHandler h = channel.pipeline().get("packet_handler");
        if (h instanceof NetworkManager) return (NetworkManager) h;
        return null;
    }

    public NetworkManager getNetworkManage(Player player) {
        return EntityNetUtils.getNetWork(EntityNetUtils.getPlayerConnection(EntityNetUtils.getNmsPlayer(player)));
    }

    public PlayerConnection getPlayerConnection(Player player) {
        return EntityNetUtils.getPlayerConnection(EntityNetUtils.getNmsPlayer(player));
    }


    public Player getPlayer(Channel channel) {
        return getPlayer(getNetworkManage(channel));
    }

    public Player getPlayer(NetworkManager network) {
        if (network == null) return null;
        PacketListener listener = newtworkPacketListener.get(network);
        if (listener instanceof PlayerConnection playerConnection){
            return connectionEntityPlayer.get(playerConnection).getBukkitEntity();
        }
        return null;
    }

    public Channel getChannel(Player player) {
        return getChannel(getNetworkManage(player));
    }

    public Channel getChannel(NetworkManager networkManager) {
        return networkChannel.get(networkManager);
    }

    public Set<Integer> getCache() {
        return cache;
    }
}
