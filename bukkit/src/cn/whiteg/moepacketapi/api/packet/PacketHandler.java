package cn.whiteg.moepacketapi.api.packet;

public abstract class PacketHandler
{
    public abstract void onPacketSending(final PacketEvent p0);
    
    public abstract void onPacketReceiving(final PacketEvent p0);
}
