package cn.whiteg.moepacketapi.utils;

import io.netty.channel.Channel;
import net.minecraft.server.v1_15_R1.EntityPlayer;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Reflection {
    public static Class getCBClass(final String name) {

        return getClass(NMSUtils.getCraftBukkit() + name);
    }

    public static Class getNMSClass(final String name) {

        return getClass(NMSUtils.getNetMinecraftServer() + name);
    }

    public static Class getClass(final String name) {
        try{
            return Class.forName(name);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static Channel getChannel(final Player p) {
        EntityPlayer np = ((CraftPlayer) p).getHandle();
        return np.playerConnection.networkManager.channel;
    }

    public static Field getFieldByName(final Class<?> parent,final String name) {
        Field tempField = null;
        for (final Field field : parent.getDeclaredFields()) {
            if (field.getName().equals(name)){
                tempField = field;
                break;
            }
        }
        if (tempField != null){
            tempField.setAccessible(true);
            return tempField;
        }
        return null;
    }

    public static Field getFieldByType(final Class<?> parent,final Class<?> type) {
        Field tempField = null;
        for (final Field field : parent.getDeclaredFields()) {
            if (field.getType().equals(type)){
                tempField = field;
                break;
            }
        }
        return tempField;
    }

    public static Method getMethod(final Class<?> parent,final String name,final Class... parameters) {
        for (final Method method : parent.getDeclaredMethods()) {
            if (method.getName().equals(name)){
                if (parameters.length == method.getParameterTypes().length){
                    boolean same = true;
                    for (int x = 0; x < method.getParameterTypes().length; ++x) {
                        if (method.getParameterTypes()[x] != parameters[x]){
                            same = false;
                            break;
                        }
                    }
                    if (same){
                        return method;
                    }
                }
            }
        }
        for (final Method method : parent.getMethods()) {
            if (method.getName().equals(name)){
                if (parameters.length == method.getParameterTypes().length){
                    boolean same = true;
                    for (int x = 0; x < method.getParameterTypes().length; ++x) {
                        if (method.getParameterTypes()[x] != parameters[x]){
                            same = false;
                            break;
                        }
                    }
                    if (same){
                        return method;
                    }
                }
            }
        }
        return null;
    }
}
