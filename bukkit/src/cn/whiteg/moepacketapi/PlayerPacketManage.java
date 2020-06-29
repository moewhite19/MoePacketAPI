package cn.whiteg.moepacketapi;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.server.v1_16_R1.NetworkManager;
import net.minecraft.server.v1_16_R1.Packet;
import net.minecraft.server.v1_16_R1.PacketListener;
import net.minecraft.server.v1_16_R1.PlayerConnection;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class PlayerPacketManage {
    private static Method networkManageRead;
    private static Field packetListener;

    static {
        try{
            networkManageRead = NetworkManager.class.getDeclaredMethod("channelRead0",ChannelHandlerContext.class,Packet.class);
            networkManageRead.setAccessible(true);
            packetListener = NetworkManager.class.getDeclaredField("packetListener");
            packetListener.setAccessible(true);
        }catch (NoSuchMethodException | NoSuchFieldException e){
            e.printStackTrace();
        }
    }

    private final Set<Integer> cache = Collections.newSetFromMap(new WeakHashMap<>());

    PlayerPacketManage() {
    }

    //向玩家发包
    public void sendPacket(Player player,Object packet) {
        if (player.isOnline() && packet instanceof Packet){
            setPluginPacket(packet);
            ((CraftPlayer) player).getHandle().playerConnection.networkManager.sendPacket((Packet<?>) packet);
        }
    }

    //是否为插件发包
    public boolean isPluginPacket(Object packet) {
        synchronized (cache) {
            return cache.contains(packet.hashCode());
        }
    }


    //设置插件发包
    public void setPluginPacket(Object packet) {
        synchronized (cache) {
            cache.add(packet.hashCode());
        }
    }

    //模拟服务端收包
    public void recieveClientPacket(Channel channel,Object packet) {
        if (!channel.isOpen()) return;
        ChannelHandler h = channel.pipeline().get("packet_handler");
        try{
            networkManageRead.invoke(h,channel.pipeline().lastContext(),packet);
            setPluginPacket(packet);
        }catch (IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
        }

        //另一种实现方式
        // if (!ctx.channel().isOpen()) return;
        //        NetworkManager networkManager = (NetworkManager) ctx.pipeline().get("packet_handler");
        //        try{
        //            networkManager.channelRead(ctx,packet);
        //            setPluginPacket(packet);
        //        }catch (Exception e){
        //            e.printStacrkTrace();
        //        }

    }

    public NetworkManager getNetworkManage(Channel channel) {
        ChannelHandler h = channel.pipeline().get("packet_handler");
        if (h instanceof NetworkManager) return (NetworkManager) h;
        return null;
    }

    public Player getPlayer(Channel channel) {
        return getPlayer(getNetworkManage(channel));
    }

    public Player getPlayer(NetworkManager network) {
        if (network == null) return null;
        try{
            PacketListener listener = (PacketListener) packetListener.get(network);
            if (listener instanceof PlayerConnection){
                return ((PlayerConnection) listener).player.getBukkitEntity();
            }
        }catch (IllegalAccessException e){
            e.printStackTrace();
        }
        return null;
    }

    public Set<Integer> getCache() {
        return cache;
    }
}
