package cn.whiteg.moepacketapi.api;

import cn.whiteg.moepacketapi.api.packet.PacketHandler;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

public class PacketManager {
    final private List<PacketHandler> packetHandles;

    public PacketManager() {
        this.packetHandles = Collections.synchronizedList(Lists.newArrayList());
    }

    public void addPacketListener(final PacketHandler handler) {
        this.packetHandles.add(handler);
    }

    public void removePacketListener(final PacketHandler handler) {
        this.packetHandles.remove(handler);
    }

    public List<PacketHandler> getPacketHandles() {
        return this.packetHandles;
    }
}
