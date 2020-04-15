# MoePacketAPI Minecraft服务器收发包API

##开发文档
```java
public class Tester extends JavaPlugin implements Listener {
    public static Logger logger;
    public static Tester plugin;
    private PacketHandler handler;

    public Tester() {
        plugin = this;
    }

    public void onLoad() {
        logger = getLogger();
    }

    public void onEnable() {
        logger.info("开始加载插件");
        Bukkit.getPluginManager().registerEvents(this,this);
        //新建Hander
        handler = new PacketHandler() {
            @Override
            public void onPacketSending(PacketEvent event) {
                Object p = event.getPacket();
                //服务器像玩家发送区块卸载包
                if (p instanceof PacketPlayOutUnloadChunk){
                    //阻止事件，玩家不会收到包
                    event.setCancelled(true);
                }
            }

            @Override
            public void onPacketReceiving(PacketEvent event) {
                Object p = event.getPacket();
                //收到玩家聊天包
                if (p instanceof PacketPlayInChat){
                    PacketPlayInChat p1 = (PacketPlayInChat) p;
                    String msg = p1.b();
                    //如果开头是"//"
                    if (msg.startsWith("//")){
                        //给玩家发送包的内容
                        event.getPlayer().sendMessage(msg);
                        //阻止事件
                        event.setCancelled(true);
                    }
                }
            }
        };
        //注册Hander
        MoePacketAPI.getInstance().getPacketManager().addPacketListener(handler);
    }

}

```



