package cn.whiteg.moepacketapi.hook;

import cn.whiteg.moepacketapi.MoePacketAPI;
import cn.whiteg.moepacketapi.utils.ReflectionUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.mojang.authlib.GameProfile;
import io.netty.channel.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Represents a very tiny alternative to ProtocolLib.
 * <p>
 * It now supports intercepting packets during login and status ping (such as OUT_SERVER_PING)!
 *
 * @author Kristian
 */
public abstract class TinyProtocol implements IHook{
    private static final AtomicInteger ID = new AtomicInteger(0);

    // Used in order to lookup a channel
    private static final ReflectionUtils.MethodInvoker getPlayerHandle = ReflectionUtils.getMethod("{obc}.entity.CraftPlayer","getHandle");
    private static final ReflectionUtils.FieldAccessor<Object> getConnection = ReflectionUtils.getField("{nms}.EntityPlayer","playerConnection",Object.class);
    private static final ReflectionUtils.FieldAccessor<Object> getManager = ReflectionUtils.getField("{nms}.PlayerConnection","networkManager",Object.class);
    private static final ReflectionUtils.FieldAccessor<Channel> getChannel = ReflectionUtils.getField("{nms}.NetworkManager",Channel.class,0);

    // Looking up ServerConnection
    private static final Class<Object> minecraftServerClass = ReflectionUtils.getUntypedClass("{nms}.MinecraftServer");
    private static final Class<Object> serverConnectionClass = ReflectionUtils.getUntypedClass("{nms}.ServerConnection");
    private static final ReflectionUtils.FieldAccessor<Object> getMinecraftServer = ReflectionUtils.getField("{obc}.CraftServer",minecraftServerClass,0);
    private static final ReflectionUtils.FieldAccessor<Object> getServerConnection = ReflectionUtils.getField(minecraftServerClass,serverConnectionClass,0);
    //过时方法
//    private static final ReflectionUtils.MethodInvoker getNetworkMarkers = ReflectionUtils.getTypedMethod(serverConnectionClass,null,List.class,serverConnectionClass);

    // Packets we have to intercept
    private static final Class<?> PACKET_LOGIN_IN_START = ReflectionUtils.getMinecraftClass("PacketLoginInStart");
    // Packets we have to intercept
    private static final ReflectionUtils.FieldAccessor<GameProfile> loginGetGameProfile = ReflectionUtils.getField(PACKET_LOGIN_IN_START,GameProfile.class,0);
    private static final Class<?> PACKET_LOGIN_OUT_SUCCESS = ReflectionUtils.getMinecraftClass("PacketLoginOutSuccess");
    private static final ReflectionUtils.FieldAccessor<GameProfile> loginSuccessGameProfile = ReflectionUtils.getField(PACKET_LOGIN_OUT_SUCCESS,GameProfile.class,0);

    // Speedup channel lookup
    private final Map<String, Channel> channelLookup = new MapMaker().weakValues().makeMap();
    // Channels that have already been removed
    private final Set<Channel> uninjectedChannels = Collections.newSetFromMap(new MapMaker().weakKeys().<Channel, Boolean>makeMap());
    // Injected channel handlers
    private final List<Channel> serverChannels = Lists.newArrayList();
    // Current handler name
    private final String handlerName;
    protected volatile boolean closed;
    protected MoePacketAPI plugin;
    private Listener listener;
    // List of network markers
    private List<Object> networkManagers;
    private ChannelInboundHandlerAdapter serverChannelHandler;
    private ChannelInitializer<Channel> beginInitProtocol;
    private ChannelInitializer<Channel> endInitProtocol;

    /**
     * Construct a new instance of TinyProtocol, and start intercepting packets for all connected clients and future clients.
     * <p>
     * You can construct multiple instances per plugin.
     *
     * @param plugin - the plugin.
     */
    public TinyProtocol(final MoePacketAPI plugin) {
        this.plugin = plugin;

        // Compute handler name
        this.handlerName = getHandlerName();

        // Prepare existing players
        registerBukkitEvents();

        try{
            registerChannelHandler();
            registerPlayers(plugin);
        }catch (IllegalArgumentException ex){
            // Damn you, late bind
            plugin.getLogger().info("[TinyProtocol] Delaying server channel injection due to late bind.");

            new BukkitRunnable() {
                @Override
                public void run() {
                    registerChannelHandler();
                    registerPlayers(plugin);
                    plugin.getLogger().info("[TinyProtocol] Late bind injection successful.");
                }
            }.runTask(plugin);
        }
    }

    private void createServerChannelHandler() {
        // Handle connected channels
        endInitProtocol = new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                try{
                    // This can take a while, so we need to stop the main thread from interfering
                    synchronized (networkManagers) {
                        // Stop injecting channels
                        if (!closed){
                            channel.eventLoop().submit(() -> injectChannelInternal(channel));
                        }
                    }
                }catch (Exception e){
                    plugin.getLogger().log(Level.SEVERE,"Cannot inject incomming channel " + channel,e);
                }
            }
        };
        // This is executed before Minecraft's channel handler
        beginInitProtocol = new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) {
                channel.pipeline().addLast(endInitProtocol);
            }
        };
        serverChannelHandler = new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx,Object msg) {
                Channel channel = (Channel) msg;
                // Prepare to initialize ths channel
                channel.pipeline().addFirst(beginInitProtocol);
                ctx.fireChannelRead(msg);
            }
        };
    }

    /**
     * Register bukkit events.
     */
    private void registerBukkitEvents() {
        listener = new Listener() {
//            WeakReference<String> errName = new WeakReference<>(null);

            @EventHandler(priority = EventPriority.LOWEST)
            public void onPlayerLogin(PlayerLoginEvent e) {
                if (closed)
                    return;
                try{
                    Channel channel = getChannel(e.getPlayer());
                    // Don't inject players that have been explicitly uninjected
                    if (!uninjectedChannels.contains(channel)){
                        injectPlayer(e.getPlayer(),channel);
                    }
                }catch (Exception exception){
                    if (plugin.getSetting().DEBUG){
                        plugin.getLogger().warning("玩家登陆时没有注册代理");
                    }
                }
            }

            @EventHandler(priority = EventPriority.LOWEST)
            public void onPlayerLogin(PlayerJoinEvent e) {
                if (closed)
                    return;
                try{
                    Channel channel = getChannel(e.getPlayer());
                    // Don't inject players that have been explicitly uninjected
                    if (!uninjectedChannels.contains(channel)){
                        injectPlayer(e.getPlayer(),channel);
                    }
                }catch (Exception exception){
                    if (plugin.getSetting().DEBUG){
                        plugin.getLogger().warning("玩家加入时没有注册代理");
                    }
                }
            }

            @EventHandler
            public void onPluginDisable(PluginDisableEvent e) {
                if (e.getPlugin().equals(plugin)){
                    close();
                }
            }
        };

        plugin.getServer().getPluginManager().registerEvents(listener,plugin);
    }

    @SuppressWarnings("unchecked")
    private void registerChannelHandler() {
        Object mcServer = getMinecraftServer.get(Bukkit.getServer());
        Object serverConnection = getServerConnection.get(mcServer);
        boolean looking = true;

        // We need to synchronize against this list
//        networkManagers = (List<Object>) getNetworkMarkers.invoke(null,serverConnection);
        try{
            Field f = serverConnectionClass.getDeclaredField("connectedChannels");
            f.setAccessible(true);
            networkManagers = (List<Object>) f.get(serverConnection);
        }catch (NoSuchFieldException | IllegalAccessException e){
            e.printStackTrace();
        }
        createServerChannelHandler();

        // Find the correct list, or implicitly throw an exception
        for (int i = 0; looking; i++) {
            List<Object> list = ReflectionUtils.getField(serverConnection.getClass(),List.class,i).get(serverConnection);

            for (Object item : list) {
                if (!ChannelFuture.class.isInstance(item))
                    break;

                // Channel future that contains the server connection
                Channel serverChannel = ((ChannelFuture) item).channel();

                serverChannels.add(serverChannel);
                serverChannel.pipeline().addFirst(serverChannelHandler);
                looking = false;
            }
        }
    }

    private void unregisterChannelHandler() {
        if (serverChannelHandler == null)
            return;

        for (Channel serverChannel : serverChannels) {
            final ChannelPipeline pipeline = serverChannel.pipeline();

            // Remove channel handler
            serverChannel.eventLoop().execute(new Runnable() {

                @Override
                public void run() {
                    try{
                        pipeline.remove(serverChannelHandler);
                    }catch (NoSuchElementException e){
                        // That's fine
                    }
                }

            });
        }
    }

    private void registerPlayers(Plugin plugin) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            injectPlayer(player);
        }
    }

    /**
     * Invoked when the server is starting to send a packet to a player.
     * <p>
     * Note that this is not executed on the main thread.
     *
     * @param receiver - the receiving player, NULL for early login/status packets.
     * @param channel  - the channel that received the packet. Never NULL.
     * @param packet   - the packet being sent.
     * @return The packet to send instead, or NULL to cancel the transmission.
     */
    public Object onPacketOutAsync(Player receiver,Channel channel,Object packet) {
        return packet;
    }

    /**
     * Invoked when the server has received a packet from a given player.
     * <p>
     * Use {@link Channel#remoteAddress()} to get the remote address of the client.
     *
     * @param sender  - the player that sent the packet, NULL for early login/status packets.
     * @param channel - channel that received the packet. Never NULL.
     * @param packet  - the packet being received.
     * @return The packet to recieve instead, or NULL to cancel.
     */
    public Object onPacketInAsync(Player sender,Channel channel,Object packet) {
        return packet;
    }

    /**
     * Send a packet to a particular player.
     * <p>
     * Note that {@link #onPacketOutAsync(Player,Channel,Object)} will be invoked with this packet.
     *
     * @param player - the destination player.
     * @param packet - the packet to send.
     */
    public void sendPacket(Player player,Object packet) {
        sendPacket(getChannel(player),packet);
    }

    /**
     * Send a packet to a particular client.
     * <p>
     * Note that {@link #onPacketOutAsync(Player,Channel,Object)} will be invoked with this packet.
     *
     * @param channel - client identified by a channel.
     * @param packet  - the packet to send.
     */
    public void sendPacket(Channel channel,Object packet) {
        channel.pipeline().writeAndFlush(packet);
    }

    /**
     * Pretend that a given packet has been received from a player.
     * <p>
     * Note that {@link #onPacketInAsync(Player,Channel,Object)} will be invoked with this packet.
     *
     * @param player - the player that sent the packet.
     * @param packet - the packet that will be received by the server.
     */
    public void receivePacket(Player player,Object packet) {
        receivePacket(getChannel(player),packet);
    }

    /**
     * Pretend that a given packet has been received from a given client.
     * <p>
     * Note that {@link #onPacketInAsync(Player,Channel,Object)} will be invoked with this packet.
     *
     * @param channel - client identified by a channel.
     * @param packet  - the packet that will be received by the server.
     */
    public void receivePacket(Channel channel,Object packet) {
        channel.pipeline().context("encoder").fireChannelRead(packet);
    }

    /**
     * Retrieve the name of the channel injector, default implementation is "tiny-" + plugin name + "-" + a unique ID.
     * <p>
     * Note that this method will only be invoked once. It is no longer necessary to override this to support multiple instances.
     *
     * @return A unique channel handler name.
     */
    protected String getHandlerName() {
        return "tiny-" + plugin.getName() + "-" + ID.incrementAndGet();
    }

    /**
     * Add a custom channel handler to the given player's channel pipeline, allowing us to intercept sent and received packets.
     * <p>
     * This will automatically be called when a player has logged in.
     *
     * @param player - the player to inject.
     */
    public void injectPlayer(Player player) {
        injectChannelInternal(getChannel(player)).player = player;
    }

    public void injectPlayer(Player player,Channel channel) {
        injectChannelInternal(channel).player = player;
    }

    /**
     * Add a custom channel handler to the given channel.
     *
     * @param channel - the channel to inject.
     * @return The intercepted channel, or NULL if it has already been injected.
     */
    public void injectChannel(Channel channel) {
        injectChannelInternal(channel);
    }

    /**
     * Add a custom channel handler to the given channel.
     *
     * @param channel - the channel to inject.
     * @return The packet interceptor.
     */
    private PacketInterceptor injectChannelInternal(Channel channel) {
        try{
            PacketInterceptor interceptor = (PacketInterceptor) channel.pipeline().get(handlerName);
            // Inject our packet interceptor
            if (interceptor == null){
                interceptor = new PacketInterceptor();
                channel.pipeline().addBefore("packet_handler",handlerName,interceptor);
                uninjectedChannels.remove(channel);
            }
            return interceptor;
        }catch (IllegalArgumentException e){
            // Try again
            return (PacketInterceptor) channel.pipeline().get(handlerName);
        }
    }

    /**
     * Retrieve the Netty channel associated with a player. This is cached.
     *
     * @param player - the player.
     * @return The Netty channel.
     */
    public Channel getChannel(Player player) {
//        return channelLookup.computeIfAbsent(player.getName(),(k) -> ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel);
        Channel channel = channelLookup.get(player.getName());
        // Lookup channel again
        if (channel == null){
            Object connection = getConnection.get(getPlayerHandle.invoke(player));
            Object manager = getManager.get(connection);
            channelLookup.put(player.getName(),channel = getChannel.get(manager));
        }
        return channel;
    }

    /**
     * Uninject a specific player.
     *
     * @param player - the injected player.
     */
    public void uninjectPlayer(Player player) {
        uninjectChannel(getChannel(player));
    }

    /**
     * Uninject a specific channel.
     * <p>
     * This will also disable the automatic channel injection that occurs when a player has properly logged in.
     *
     * @param channel - the injected channel.
     */
    public void uninjectChannel(final Channel channel) {
        // No need to guard against this if we're closing
        if (!closed){
            uninjectedChannels.add(channel);
        }
        // See ChannelInjector in ProtocolLib, line 590
        channel.eventLoop().execute(new Runnable() {
            @Override
            public void run() {
                channel.pipeline().remove(handlerName);
            }

        });
    }

    /**
     * Determine if the given player has been injected by TinyProtocol.
     *
     * @param player - the player.
     * @return TRUE if it is, FALSE otherwise.
     */
    public boolean hasInjected(Player player) {
        return hasInjected(getChannel(player));
    }

    /**
     * Determine if the given channel has been injected by TinyProtocol.
     *
     * @param channel - the channel.
     * @return TRUE if it is, FALSE otherwise.
     */
    public boolean hasInjected(Channel channel) {
        return channel.pipeline().get(handlerName) != null;
    }

    /**
     * Cease listening for packets. This is called automatically when your plugin is disabled.
     */
    @Override
    public final void close() {
        if (!closed){
            closed = true;

            // Remove our handlers
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                uninjectPlayer(player);
            }
            // Clean up Bukkit
            HandlerList.unregisterAll(listener);
            unregisterChannelHandler();
        }
    }

    /**
     * Channel handler that is inserted into the player's channel pipeline, allowing us to intercept sent and received packets.
     *
     * @author Kristian
     */
    private final class PacketInterceptor extends ChannelDuplexHandler {
        // Updated by the login event
        public volatile Player player;

        @Override
        public void channelRead(ChannelHandlerContext ctx,Object msg) throws Exception {
            // Intercept channel
            final Channel channel = ctx.channel();
            try{
                msg = onPacketInAsync(player,channel,msg);
            }catch (Exception e){
                plugin.getLogger().log(Level.SEVERE,"Error in onPacketInAsync().",e);
            }

            if (msg != null){
                handleLoginStart(this,channel,msg);
                super.channelRead(ctx,msg);
            }
        }

        @Override
        public void write(ChannelHandlerContext ctx,Object msg,ChannelPromise promise) throws Exception {
            final Channel channel = ctx.channel();
            try{
                msg = onPacketOutAsync(player,ctx.channel(),msg);
            }catch (Exception e){
                plugin.getLogger().log(Level.SEVERE,"Error in onPacketOutAsync().",e);
            }
            if (msg != null){
                handleLoginSuccess(this,channel,msg);
                super.write(ctx,msg,promise);
            }
        }

        //
//        @Override
//        public void close(ChannelHandlerContext ctx,ChannelPromise promise) throws Exception {
//            if (player != null){
//                channelLookup.remove(player.getName());
//                if (plugin.getSetting().DEBUG){
//                    plugin.getLogger().info("删除Channel " + ctx.channel().id().toString() + " " + player.getName());
//                }
//            }
//            super.close(ctx,promise);
//        }
        private void handleLoginStart(PacketInterceptor ctx,Channel channel,Object packet) {
            if (player != null) return;
            if (PACKET_LOGIN_IN_START.isInstance(packet)){
                GameProfile profile = loginGetGameProfile.get(packet);
                channelLookup.put(profile.getName(),channel);
                if (plugin.getSetting().DEBUG){
                    plugin.getLogger().info("开始认证，储存Channel " + channel.id().toString() + " " + profile.getName());
                }
            }
        }

        private void handleLoginSuccess(PacketInterceptor ctx,Channel channel,Object packet) {
            if (player != null) return;
            if (PACKET_LOGIN_OUT_SUCCESS.isInstance(packet)){
                GameProfile profile = loginSuccessGameProfile.get(packet);
                channelLookup.put(profile.getName(),channel);
                if (plugin.getSetting().DEBUG){
                    plugin.getLogger().info("认证完成，储存Channel " + channel.id().toString() + " " + profile.getName());
                }
            }
        }

    }
}
