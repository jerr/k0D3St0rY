package org.k0D3St0rY.cs2013.server;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.buffer.ChannelBuf;
import io.netty.buffer.MessageBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInboundMessageHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.aio.AioEventLoopGroup;
import io.netty.channel.socket.aio.AioServerSocketChannel;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Little Openshift local addr hacking.
 * Openshift server don't authorize 127.0.0.1 binding.
 */
public class HttpCSServerBootstrap extends AbstractBootstrap<HttpCSServerBootstrap> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(HttpCSServerBootstrap.class);
    private InetSocketAddress localAddr;

    private final ChannelHandler acceptor = new ChannelInitializer<Channel>() {
        @Override
        public void initChannel(Channel ch) throws Exception {
            ch.pipeline().addLast(new Acceptor());
        }
    };

    private final Map<ChannelOption<?>, Object> childOptions = new LinkedHashMap<ChannelOption<?>, Object>();
    private final Map<AttributeKey<?>, Object> childAttrs = new LinkedHashMap<AttributeKey<?>, Object>();
    private EventLoopGroup childGroup;
    private ChannelHandler childHandler;

    public HttpCSServerBootstrap(String bind) {
        super();
        localAddr = new InetSocketAddress(bind, 0);

    }

    /**
     * Specify the {@link EventLoopGroup} which is used for the parent (acceptor) and the child (client).
     */
    @Override
    public HttpCSServerBootstrap group(EventLoopGroup group) {
        return group(group, group);
    }

    /**
     * Set the {@link EventLoopGroup} for the parent (acceptor) and the child (client). These {@link EventLoopGroup}'s are used
     * to handle all the events and IO for {@link SocketChannel} and {@link Channel}'s.
     */
    public HttpCSServerBootstrap group(EventLoopGroup parentGroup, EventLoopGroup childGroup) {
        super.group(parentGroup);
        if (childGroup == null) {
            throw new NullPointerException("childGroup");
        }
        if (this.childGroup != null) {
            throw new IllegalStateException("childGroup set already");
        }
        this.childGroup = childGroup;
        return this;
    }

    /**
     * The {@link Class} which is used to create the {@link ServerChannel} from (for the acceptor).
     */
    @Override
    public HttpCSServerBootstrap channel(Class<? extends Channel> channelClass) {
        if (channelClass == null) {
            throw new NullPointerException("channelClass");
        }
        if (!ServerChannel.class.isAssignableFrom(channelClass)) {
            throw new IllegalArgumentException("channelClass must be subtype of " + ServerChannel.class.getSimpleName() + '.');
        }
        if (channelClass == AioServerSocketChannel.class) {
            return channelFactory(new AioServerSocketChannelFactory());
        }
        return super.channel(channelClass);
    }

    /**
     * Allow to specify a {@link ChannelOption} which is used for the {@link Channel} instances once they get created (after the
     * acceptor accepted the {@link Channel}). Use a value of {@code null} to remove a previous set {@link ChannelOption}.
     */
    public <T> HttpCSServerBootstrap childOption(ChannelOption<T> childOption, T value) {
        if (childOption == null) {
            throw new NullPointerException("childOption");
        }
        if (value == null) {
            childOptions.remove(childOption);
        } else {
            childOptions.put(childOption, value);
        }
        return this;
    }

    public <T> HttpCSServerBootstrap childAttr(AttributeKey<T> childKey, T value) {
        if (childKey == null) {
            throw new NullPointerException("childKey");
        }
        if (value == null) {
            childAttrs.remove(childKey);
        } else {
            childAttrs.put(childKey, value);
        }
        return this;
    }

    /**
     * Set the {@link ChannelHandler} which is used to server the request for the {@link Channel}'s.
     */
    public HttpCSServerBootstrap childHandler(ChannelHandler childHandler) {
        if (childHandler == null) {
            throw new NullPointerException("childHandler");
        }
        this.childHandler = childHandler;
        return this;
    }

    @Override
    public ChannelFuture bind(ChannelFuture future) {
        validate(future);
        Channel channel = future.channel();
        if (channel.isActive()) {
            future.setFailure(new IllegalStateException("channel already bound: " + channel));
            return future;
        }
        if (channel.isRegistered()) {
            future.setFailure(new IllegalStateException("channel already registered: " + channel));
            return future;
        }
        if (!channel.isOpen()) {
            future.setFailure(new ClosedChannelException());
            return future;
        }

        try {
            channel.config().setOptions(options());
        } catch (Exception e) {
            future.setFailure(e);
            return future;
        }

        for (Entry<AttributeKey<?>, Object> e : attrs().entrySet()) {
            @SuppressWarnings("unchecked")
            AttributeKey<Object> key = (AttributeKey<Object>) e.getKey();
            channel.attr(key).set(e.getValue());
        }

        ChannelPipeline p = future.channel().pipeline();
        if (handler() != null) {
            p.addLast(handler());
        }
        p.addLast(acceptor);

        ChannelFuture f = group().register(channel).awaitUninterruptibly();
        if (!f.isSuccess()) {
            future.setFailure(f.cause());
            return future;
        }

        if (!ensureOpen(future)) {
            return future;
        }

        channel.bind(localAddress(), future).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

        return future;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (childGroup != null) {
            childGroup.shutdown();
        }
    }

    @Override
    protected void validate() {
        super.validate();
        if (childHandler == null) {
            throw new IllegalStateException("childHandler not set");
        }
        if (childGroup == null) {
            logger.warn("childGroup is not set. Using parentGroup instead.");
            childGroup = group();
        }
        if (localAddress() == null) {
            logger.warn("localAddress is not set. Using " + localAddr + " instead.");
            localAddress(localAddr);
        }
    }

    private class Acceptor extends ChannelInboundHandlerAdapter implements ChannelInboundMessageHandler<Channel> {

        public MessageBuf<Channel> newInboundBuffer(ChannelHandlerContext ctx) throws Exception {
            return Unpooled.messageBuffer();
        }

        public void freeInboundBuffer(ChannelHandlerContext ctx, ChannelBuf buf) throws Exception {
            // Nothing to free
        }

        @SuppressWarnings("unchecked")
        @Override
        public void inboundBufferUpdated(ChannelHandlerContext ctx) {
            MessageBuf<Channel> in = ctx.inboundMessageBuffer();
            for (;;) {
                Channel child = in.poll();
                if (child == null) {
                    break;
                }

                child.pipeline().addLast(childHandler);

                for (Entry<ChannelOption<?>, Object> e : childOptions.entrySet()) {
                    try {
                        if (!child.config().setOption((ChannelOption<Object>) e.getKey(), e.getValue())) {
                            logger.warn("Unknown channel option: " + e);
                        }
                    } catch (Throwable t) {
                        logger.warn("Failed to set a channel option: " + child, t);
                    }
                }

                for (Entry<AttributeKey<?>, Object> e : childAttrs.entrySet()) {
                    child.attr((AttributeKey<Object>) e.getKey()).set(e.getValue());
                }

                try {
                    childGroup.register(child);
                } catch (Throwable t) {
                    child.unsafe().closeForcibly();
                    logger.warn("Failed to register an accepted channel: " + child, t);
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(super.toString());
        buf.setLength(buf.length() - 1);
        buf.append(", ");
        if (childGroup != null) {
            buf.append("childGroup: ");
            buf.append(childGroup.getClass().getSimpleName());
            buf.append(", ");
        }
        if (childOptions != null && !childOptions.isEmpty()) {
            buf.append("childOptions: ");
            buf.append(childOptions);
            buf.append(", ");
        }
        if (childAttrs != null && !childAttrs.isEmpty()) {
            buf.append("childAttrs: ");
            buf.append(childAttrs);
            buf.append(", ");
        }
        if (childHandler != null) {
            buf.append("childHandler: ");
            buf.append(childHandler);
            buf.append(", ");
        }
        if (buf.charAt(buf.length() - 1) == '(') {
            buf.append(')');
        } else {
            buf.setCharAt(buf.length() - 2, ')');
            buf.setLength(buf.length() - 1);
        }

        return buf.toString();
    }

    private final class AioServerSocketChannelFactory implements ChannelFactory {
        public Channel newChannel() {
            return new AioServerSocketChannel((AioEventLoopGroup) group(), (AioEventLoopGroup) childGroup);
        }

        @Override
        public String toString() {
            return AioServerSocketChannel.class.getSimpleName() + ".class";
        }
    }
}
