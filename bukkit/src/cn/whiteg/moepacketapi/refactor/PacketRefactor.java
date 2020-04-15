package cn.whiteg.moepacketapi.refactor;

import cn.whiteg.moepacketapi.utils.NMSUtils;

public class PacketRefactor {
    private static IPacketRefactor refactor;

    static {
        switch (NMSUtils.getServerVersion()) {
            case "v1_14_R1": {
//                PacketRefactor.refactor = new PacketRefactor_v1_14_R1();
                break;
            }
            case "v1_15_R1": {
                PacketRefactor.refactor = new PacketRefactor_v1_15_R1();
                break;
            }
        }
    }

    public static IPacketRefactor getRefactor() {
        return PacketRefactor.refactor;
    }
}
