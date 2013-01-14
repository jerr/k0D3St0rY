package org.k0D3St0rY.cs2013.server;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class HttpCSServerHandler extends ChannelInboundMessageHandlerAdapter<Object> {

    @Override
    public void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {

        StringBuilder buffer = new StringBuilder();
        
        HttpRequest request = (HttpRequest) msg;
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
        Map<String, List<String>> params = queryStringDecoder.getParameters();
        System.out.println("## " + ctx.channel().toString() +  " - " + request.getUri() );
        if (!params.isEmpty() && params.containsKey("q") && params.get("q").contains("Quelle est ton adresse email")) {
            buffer.append("jeremie").append(".").append("code").append("story").append("@").append("gmail").append(".").append("com");
            System.out.println(" >> sending email! " + new Date() );
        }
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.setContent(Unpooled.copiedBuffer(buffer, CharsetUtil.UTF_8));
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");

        // Write the response.
        ChannelFuture future = ctx.write(response);

        // Close the connection after the write operation is done.
        future.addListener(ChannelFutureListener.CLOSE);

    }
}
