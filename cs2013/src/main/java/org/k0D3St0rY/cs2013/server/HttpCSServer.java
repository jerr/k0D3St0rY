package org.k0D3St0rY.cs2013.server;

import java.net.SocketAddress;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.http.HttpServerCodec;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class HttpCSServer extends AbstractIdleService {

    static final InternalLogger logger = InternalLoggerFactory.getInstance(HttpCSServer.class);

    private final ChannelGroup allChannels;
    private final SocketAddress address;
    private final ChannelFactory factory;
    private final ServerBootstrap bootstrap;
    private final Provider<HttpCSHandler> handler;

    @Inject
    HttpCSServer(ChannelFactory factory, ChannelGroup allChannels, SocketAddress address, Provider<HttpCSHandler> handler) {
        this.factory = factory;
        this.bootstrap = new ServerBootstrap(factory);
        this.allChannels = allChannels;
        this.address = address;
        this.handler = handler;
    }

    @Override
    protected void startUp() throws Exception {
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(new HttpServerCodec(), handler.get());
            }
        });
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);
        Channel channel = bootstrap.bind(address);
        allChannels.add(channel);
    }

    @Override
    protected void shutDown() throws Exception {
        allChannels.close().awaitUninterruptibly();
        factory.releaseExternalResources();
    }
}
