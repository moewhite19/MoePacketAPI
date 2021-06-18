package cn.whiteg.moepacketapi.utils;

import io.netty.channel.Channel;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.PlayerConnection;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;

public class EntityNetUtils {
    static ReflectionUtils.FieldAccessor<PlayerConnection> entity_counter;
    static ReflectionUtils.FieldAccessor<NetworkManager> connect_network;
    static ReflectionUtils.FieldAccessor<Channel> network_channel;


    static {
        try{
            entity_counter = ReflectionUtils.getFieldFormType(EntityPlayer.class,PlayerConnection.class);
            connect_network = ReflectionUtils.getFieldFormType(PlayerConnection.class,NetworkManager.class);
            network_channel = ReflectionUtils.getFieldFormType(NetworkManager.class,Channel.class);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static PlayerConnection getPlayerConnection(EntityPlayer player) {
        return entity_counter.get(player);
    }

    public static NetworkManager getNetWork(PlayerConnection playerConnection) {
        return connect_network.get(playerConnection);
    }

    //获取玩家网络通道
    public static Channel getChannel(NetworkManager networkManager) {
        return network_channel.get(networkManager);
    }

    //根据id获取实体
    public static Entity getEntityById(World world,int id) {
        try{
            WorldServer nmsWorld = (WorldServer) world.getClass().getMethod("getHandle").invoke(world);
            var entity = nmsWorld.getEntity(id);
            return entity == null ? null : entity.getBukkitEntity();
        }catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    public static net.minecraft.world.entity.Entity getNmsEntity(org.bukkit.entity.Entity bukkitEntity) {
        try{
            //noinspection ResultOfMethodCallIgnored
            return (net.minecraft.world.entity.Entity) bukkitEntity.getClass().getMethod("getHandle").invoke(bukkitEntity);
        }catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e){
            throw new RuntimeException(e);
        }
    }

    public static EntityPlayer getNmsPlayer(Player player) {
        return (EntityPlayer) getNmsEntity(player);
    }
}
