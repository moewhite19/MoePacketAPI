package cn.whiteg.moepacketapi.refactor;

import cn.whiteg.moepacketapi.api.event.PlayerJumpEvent;
import cn.whiteg.moepacketapi.utils.Reflection;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.server.v1_15_R1.PacketPlayInChat;
import net.minecraft.server.v1_15_R1.PacketPlayInFlying;
import net.minecraft.server.v1_15_R1.PacketPlayOutUnloadChunk;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.math.RoundingMode;
import java.text.NumberFormat;

public class PacketRefactor_v1_15_R1 implements IPacketRefactor {
    @Override
    public boolean refact(final Object packet,final Player p,ChannelHandlerContext ctx) {
        try{
            if (packet instanceof PacketPlayInFlying){
                final PacketPlayInFlying raw = (PacketPlayInFlying) packet;
                final double yDiff = (double) Reflection.getFieldByName(raw.getClass(),"x").get(raw);
//                final double yDiff = raw.a(0);
                final NumberFormat nf = NumberFormat.getNumberInstance();
                nf.setMaximumFractionDigits(14);
                nf.setRoundingMode(RoundingMode.DOWN);
                final String str = nf.format(yDiff);
                Double.valueOf(str.replace(",","."));
                final double num = Double.valueOf(str.replace(",","."));
                if (num == 0.41999998688697){
                    final PlayerJumpEvent event = new PlayerJumpEvent(p);
                    Bukkit.getPluginManager().callEvent(event);
                    p.sendMessage("跳跃");
                    if (event.isCancelled()) return true;
                }
            } else if (packet instanceof PacketPlayInChat){
                PacketPlayInChat raw = (PacketPlayInChat) packet;
                String msg = raw.b();
                if (msg.startsWith("/")){
                    p.sendMessage(raw.b());
//                    return true;
                }
            } else if (packet instanceof PacketPlayOutUnloadChunk){
                PacketPlayOutUnloadChunk raw = (PacketPlayOutUnloadChunk) packet;
                p.sendActionBar("阻止卸载区块");
                return true;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
}
