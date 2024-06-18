package cn.whiteg.moepacketapi.utils;

import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;

public class EntityNetUtils {
    static FieldAccessor<ServerGamePacketListenerImpl> entity_counter;
    static FieldAccessor<Connection> connect_network;
    static FieldAccessor<Channel> network_channel;

    static {
        try{
            entity_counter = ReflectionUtils.getFieldFormType(ServerPlayer.class,ServerGamePacketListenerImpl.class);
            connect_network = ReflectionUtils.getFieldFormType(ServerGamePacketListenerImpl.class,Connection.class);
            network_channel = ReflectionUtils.getFieldFormType(Connection.class,Channel.class);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static ServerGamePacketListenerImpl getPlayerConnection(net.minecraft.world.entity.player.Player player) {
        return entity_counter.get(player);
    }

    public static Connection getNetWork(ServerGamePacketListenerImpl playerConnection) {
        return connect_network.get(playerConnection);
    }

    //获取玩家网络通道
    public static Channel getChannel(Connection networkManager) {
        return network_channel.get(networkManager);
    }

    //根据id获取实体
    public static Entity getEntityById(World world,int id) {
        try{
            ServerLevel nmsWorld = (ServerLevel) world.getClass().getMethod("getHandle").invoke(world);
            var entity = nmsWorld.getEntity(id);
            return entity == null ? null : entity.getBukkitEntity();
        }catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    public static net.minecraft.world.entity.Entity getNmsEntity(org.bukkit.entity.Entity bukkitEntity) {
        return ((CraftEntity) bukkitEntity).getHandle();
    }

    public static net.minecraft.world.entity.player.Player getNmsPlayer(Player player) {
        return (net.minecraft.world.entity.player.Player) getNmsEntity(player);
    }


    public static DedicatedServer getNmsServer() {
        return ((CraftServer) Bukkit.getServer()).getServer();
    }

    public static ServerLevel getNmsWorld(World world) {
        return ((CraftWorld) world).getHandle();
    }

}
