package cn.whiteg.moepacketapi.hook;

import cn.whiteg.moepacketapi.MoePacketAPI;
import cn.whiteg.moepacketapi.api.event.PacketReceiveEvent;
import cn.whiteg.moepacketapi.api.event.PacketSendEvent;
import cn.whiteg.moepacketapi.utils.EntityNetUtils;
import cn.whiteg.moepacketapi.utils.FieldAccessor;
import cn.whiteg.moepacketapi.utils.ReflectionUtils;
import com.google.common.collect.Lists;
import io.netty.channel.*;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConnection;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;

/**
 * Represents a very tiny alternative to ProtocolLib.
 * <p>
 * It now supports intercepting packets during login and status ping (such as OUT_SERVER_PING)!
 *
 * @author Kristian
 */
public class TinyProtocol implements IHook {

    private static final FieldAccessor<ServerConnection> getServerConnection;

    static {
        try{
            getServerConnection = ReflectionUtils.getFieldFormType(MinecraftServer.class,ServerConnection.class);
        }catch (NoSuchFieldException e){
            throw new RuntimeException(e);
        }
    }

    // Injected channel handlers
    private final List<Channel> serverChannels = Lists.newArrayList();
    // Current handler name
    private final String handlerName;
    protected volatile boolean closed;
    protected MoePacketAPI plugin;
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
        this.handlerName = "tiny-" + plugin.getName();
        try{
            registerChannelHandler();
        }catch (IllegalArgumentException ex){
            // Damn you, late bind
            plugin.getLogger().info("[TinyProtocol] Delaying server channel injection due to late bind.");
            registerChannelHandler();
            plugin.getLogger().info("[TinyProtocol] Late bind injection successful.");
        }
    }

    private void createServerChannelHandler() {
        // Handle connected channels
        endInitProtocol = new ChannelInitializer<Channel>() {
            @SuppressWarnings("SynchronizeOnNonFinalField")
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

    @SuppressWarnings("unchecked")
    private void registerChannelHandler() {
        Object mcServer = EntityNetUtils.getNmsServer();
        Object serverConnection = getServerConnection.get(mcServer);
        try{
            FieldAccessor<NetworkManager> f = (FieldAccessor<NetworkManager>) ReflectionUtils.getFieldFormType(ServerConnection.class,ReflectionUtils.markGenericTypes(List.class,NetworkManager.class));
            networkManagers = (List<Object>) f.get(serverConnection);
        }catch (NoSuchFieldException e){
            e.printStackTrace();
            return;
        }
        createServerChannelHandler();

        List<ChannelFuture> list = ReflectionUtils.getField(serverConnection.getClass(),List.class).get(serverConnection);
        for (ChannelFuture channelFuture : list) {
            Channel serverChannel = channelFuture.channel();
            try{
                serverChannel.pipeline().addFirst(serverChannelHandler);
                serverChannels.add(serverChannel);
                plugin.getLogger().info("开始监听" + serverChannel);
            }catch (ChannelPipelineException ignored){
                plugin.getLogger().warning("无法监听" + serverChannel);
                //如果不是Sharable会抛出异常
            }
        }
    }

    private void unregisterChannelHandler() {
        if (serverChannelHandler == null) return;

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


    /**
     * Pretend that a given packet has been received from a given client.
     * <p>
     *
     * @param channel - client identified by a channel.
     * @param packet  - the packet that will be received by the server.
     */
    public void receivePacket(Channel channel,Object packet) {
        channel.pipeline().context("encoder").fireChannelRead(packet);
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
        //超级套娃
        return EntityNetUtils.getChannel(EntityNetUtils.getNetWork(EntityNetUtils.getPlayerConnection(EntityNetUtils.getNmsPlayer(player))));
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
            unregisterChannelHandler();
        }
    }

    /**
     * Channel handler that is inserted into the player's channel pipeline, allowing us to intercept sent and received packets.
     *
     * @author Kristian
     */
    public static class PacketInterceptor extends ChannelDuplexHandler implements PlayerPacketHook {
        Player player = null;
        NetworkManager networkManager = null;

        @Override
        public void channelRead(ChannelHandlerContext ctx,Object msg) throws Exception {
            PacketReceiveEvent event = new PacketReceiveEvent((Packet<?>) msg,ctx,this);
            if (!event.callEvent()) return;
            super.channelRead(ctx,event.getPacket());
        }

        @Override
        public void write(ChannelHandlerContext ctx,Object msg,ChannelPromise promise) throws Exception {
            PacketSendEvent event = new PacketSendEvent((Packet<?>) msg,ctx,this);
            if (!event.callEvent()) return;
            super.write(ctx,event.getPacket(),promise);
        }

        @Override
        public Player getPlayer() {
            return player;
        }

        @Override
        public void setPlayer(Player player) {
            this.player = player;
        }

        @Override
        public NetworkManager getNetworkManager() {
            return networkManager;
        }

        @Override
        public void setNetworkManager(NetworkManager networkManager) {
            this.networkManager = networkManager;
        }
    }
}
