package cn.whiteg.moepacketapi.utils;

import io.netty.channel.Channel;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.PlayerConnection;
import org.apache.http.util.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;

public class EntityNetUtils {
    static FieldAccessor<PlayerConnection> entity_counter;
    static FieldAccessor<NetworkManager> connect_network;
    static FieldAccessor<Channel> network_channel;


    private static FieldAccessor<net.minecraft.world.entity.Entity> craftEntity;
    private static FieldAccessor<WorldServer> craftWorld;

    private static FieldAccessor<DedicatedServer> craftServer;

    private static String craftRoot;

    static {
        try{
            entity_counter = ReflectionUtils.getFieldFormType(EntityPlayer.class,PlayerConnection.class);
            connect_network = ReflectionUtils.getFieldFormType(PlayerConnection.class,NetworkManager.class);
            network_channel = ReflectionUtils.getFieldFormType(NetworkManager.class,Channel.class);
        }catch (Exception e){
            e.printStackTrace();
        }


        try{
            craftRoot = Bukkit.getServer().getClass().getPackage().getName();
            //从Bukkit实体获取Nms实体
            var clazz = EntityUtils.class.getClassLoader().loadClass(craftRoot + ".entity.CraftEntity");
            craftEntity = ReflectionUtils.getFieldFormType(clazz,net.minecraft.world.entity.Entity.class);
            //获取world的Nms
            clazz = EntityUtils.class.getClassLoader().loadClass(craftRoot + ".CraftWorld");
            craftWorld = ReflectionUtils.getFieldFormType(clazz,WorldServer.class);


            clazz = EntityUtils.class.getClassLoader().loadClass(craftRoot + ".CraftServer");
            craftServer = ReflectionUtils.getFieldFormType(clazz,DedicatedServer.class);
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
            var entity = nmsWorld.a(id);
            return entity == null ? null : entity.getBukkitEntity();
        }catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    public static net.minecraft.world.entity.Entity getNmsEntity(org.bukkit.entity.Entity bukkitEntity) {
        return craftEntity.get(bukkitEntity);
    }

    public static EntityPlayer getNmsPlayer(Player player) {
        return (EntityPlayer) getNmsEntity(player);
    }


    public static DedicatedServer getNmsServer() {
        return craftServer.get(Bukkit.getServer());
    }

    public static WorldServer getNmsWorld(World world) {
        return craftWorld.get(world);
    }

    public static String getCraftRoot() {
        return craftRoot;
    }

}
