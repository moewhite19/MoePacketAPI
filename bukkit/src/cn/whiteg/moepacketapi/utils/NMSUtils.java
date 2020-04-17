package cn.whiteg.moepacketapi.utils;

import org.bukkit.Bukkit;

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
}
