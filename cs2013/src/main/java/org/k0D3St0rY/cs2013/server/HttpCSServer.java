package org.k0D3St0rY.cs2013.server;

import java.net.SocketAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpServerCodec;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.k0D3St0rY.cs2013.service.ServiceFactory;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class HttpCSServer extends AbstractIdleService {
    private final ChannelGroup allChannels;
    private final SocketAddress address;
    private final ChannelFactory factory;
    private final ServerBootstrap bootstrap;
    private final Provider<Handler> handler;

    @Inject
    HttpCSServer(ChannelFactory factory, ChannelGroup allChannels, SocketAddress address, Provider<Handler> handler) {
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

class Handler extends SimpleChannelHandler {
    private final ChannelGroup allChannels;
    private final ServiceFactory serviceFactory;

    @Inject
    Handler(ChannelGroup allChannels, ServiceFactory serviceFactory) {
        this.allChannels = allChannels;
        this.serviceFactory = serviceFactory;
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        allChannels.add(e.getChannel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        e.getCause().printStackTrace();
        e.getChannel().close();
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        HttpRequest request = (HttpRequest) e.getMessage();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
        Map<String, List<String>> params = queryStringDecoder.getParameters();
        CharSequence result = serviceFactory.create("/").execute(params);
        System.out.println("## " + request.getUri() + " >> " + result);
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.setContent(ChannelBuffers.copiedBuffer(result, Charsets.UTF_8));
        e.getChannel().write(response).addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
                future.getChannel().close();
            }
        });
    }

}