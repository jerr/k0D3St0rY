package org.k0D3St0rY.cs2013.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class HttpCSServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    public void initChannel(SocketChannel channel) throws Exception {
        // Create a default pipeline implementation.
        ChannelPipeline p = channel.pipeline();

        // Register http request decoder.
        p.addLast("decoder", new HttpRequestDecoder());

        // Register http response encoder.
        p.addLast("encoder", new HttpResponseEncoder());

        // Active automatic content compression.
        p.addLast("deflater", new HttpContentCompressor());

        // Register serveur handler.
        p.addLast("handler", new HttpCSServerHandler());
    }

}