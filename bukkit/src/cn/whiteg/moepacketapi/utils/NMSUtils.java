package cn.whiteg.moepacketapi.utils;

import io.netty.channel.Channel;
import net.minecraft.server.v1_16_R3.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class NMSUtils {
    private static final String serverVersion;
    private static final String craftBukkit;
    private static final String netMinecraftServer;

    static {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        serverVersion = packageName.substring(packageName.lastIndexOf('.') + 1);
    }

    static {
        craftBukkit = "org.bukkit.craftbukkit." + serverVersion + ".";
        netMinecraftServer = "net.minecraft.server." + serverVersion + ".";
    }

    public static String getCraftBukkit() {
        return NMSUtils.craftBukkit;
    }

    public static String getNetMinecraftServer() {
        return NMSUtils.netMinecraftServer;
    }

    public static String getServerVersion() {
        return serverVersion;
    }

    public static Channel getChannel(Player player) {
        return ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel;
    }

    public static Entity getEntityById(World world,int id) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        net.minecraft.server.v1_16_R3.Entity entity = worldServer.getEntity(id);
        return entity == null ? null : entity.getBukkitEntity();
    }
}
