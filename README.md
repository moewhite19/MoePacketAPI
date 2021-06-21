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
        //注册本类事件
        Bukkit.getPluginManager().registerEvents(this,this);
    }
    //注册发包事件
    @EventHandler
    public void onPacket(PacketSendEvent event) {
        Object p = event.getPacket();
        //服务器像玩家发送区块卸载包
        if (p instanceof PacketPlayOutUnloadChunk){
            //阻止事件，玩家不会收到包
            event.setCancelled(true);
        }
    }
    //注册解析包事件
    @EventHandler
    public void onPacket(PacketReceiveEvent event) {
        Object p = event.getPacket();
        if (p instanceof PacketPlayInChat){ //玩家发送聊天包
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

}

```



