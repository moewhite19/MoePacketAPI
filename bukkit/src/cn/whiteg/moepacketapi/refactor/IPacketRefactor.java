package cn.whiteg.moepacketapi.refactor;

import io.netty.channel.ChannelHandlerContext;
import org.bukkit.entity.*;

public interface IPacketRefactor
{
    boolean refact(final Object p0,final Player p1,ChannelHandlerContext ctx);
}
