package org.k0D3St0rY.cs2013.server;

import static org.jboss.netty.handler.codec.http.HttpHeaders.is100ContinueExpected;
import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.buffer.ChannelBufferIndexFinder;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.handler.codec.http.multipart.DiskFileUpload;
import org.jboss.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.util.CharsetUtil;
import org.k0D3St0rY.cs2013.service.ServiceFactory;

import com.google.common.base.Charsets;
import com.google.inject.Inject;

public class HttpCSHandler extends SimpleChannelUpstreamHandler {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(HttpCSHandler.class);

    private final ServiceFactory serviceFactory;

    private HttpRequest request;
    private boolean readingChunks;
    private HttpPostRequestDecoder decoder;

    static {
        DiskFileUpload.deleteOnExitTemporaryFile = true;
        DiskFileUpload.baseDirectory = null;
    }

    @Inject
    HttpCSHandler(ChannelGroup allChannels, ServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        if (decoder != null) {
            decoder.cleanFiles();
        }
    }

    /** Buffer that stores the upload content */
    private final StringBuilder buf = new StringBuilder();

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (!readingChunks) {
            HttpRequest request = this.request = (HttpRequest) e.getMessage();

            if (is100ContinueExpected(request)) {
                send100Continue(e);
            }
            logger.info("## " + request.getUri() + " ## " + request.getMethod());
            URI uri = new URI(request.getUri());

            if (HttpMethod.GET.equals(request.getMethod())) {
                QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
                Map<String, List<String>> params = queryStringDecoder.getParameters();
                CharSequence result = serviceFactory.create(uri.getPath()).execute(params);
                logger.info("## " + request.getUri() + " >> " + result);
                HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                response.setContent(ChannelBuffers.copiedBuffer(result, Charsets.UTF_8));
                response.setHeader(CONTENT_LENGTH, response.getContent().readableBytes());
                e.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
                return;
            }

            if (HttpMethod.POST.equals(request.getMethod())) {
                buf.setLength(0);
                logger.debug("===================================\r\n");

                logger.debug("VERSION: " + request.getProtocolVersion() + "\r\n");
                // logger.debug("HOSTNAME: " + request.getHost(request, "unknown") + "\r\n");
                logger.debug("REQUEST_URI: " + request.getUri() + "\r\n\r\n");

                for (Map.Entry<String, String> h : request.getHeaders()) {
                    logger.debug("HEADER: " + h.getKey() + " = " + h.getValue() + "\r\n");
                }

                QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
                Map<String, List<String>> params = queryStringDecoder.getParameters();
                if (!params.isEmpty()) {
                    for (Entry<String, List<String>> p : params.entrySet()) {
                        String key = p.getKey();
                        List<String> vals = p.getValue();
                        for (String val : vals) {
                            logger.debug("PARAM: " + key + " = " + val + "\r\n");
                        }
                    }
                }
                if (request.isChunked()) {
                    readingChunks = true;
                } else {
                    ChannelBuffer content = request.getContent();
                    if (content.readable()) {
                        buf.append(content.toString(CharsetUtil.UTF_8) + "\r\n");
                    }
                    writeResponse(e);
                }
            }
        } else {
            HttpChunk chunk = (HttpChunk) e.getMessage();
            if (chunk.isLast()) {
                readingChunks = false;
                writeResponse(e);
            } else {
                buf.append(chunk.getContent().toString(CharsetUtil.UTF_8));
            }
        }
    }

    private void writeResponse(MessageEvent e) {
        logger.info("## " + request.getUri() + " ## " + request.getMethod());
        URI uri;
        String result = "";
        try {
            uri = new URI(request.getUri());
            List<String> list = new ArrayList<String>();
            list.add(buf.toString());
            Map<String, List<String>> params = new HashMap<String, List<String>>();
            params.put("content", list);
            logger.info("## path: " + uri.getPath() + " ## " + serviceFactory);
            result = (String) serviceFactory.create(uri.getPath()).execute(params);
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        }

        // Decide whether to close the connection or not.
        boolean keepAlive = isKeepAlive(request);

        // Build the response object.
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, CREATED);
        // response.setContent(ChannelBuffers.copiedBuffer(buf.toString(), CharsetUtil.UTF_8));
        response.setHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");

        if (keepAlive) {
            // Add 'Content-Length' header only for a keep-alive connection.
            response.setContent(ChannelBuffers.copiedBuffer(result, Charsets.UTF_8));
            response.setHeader(CONTENT_LENGTH, response.getContent().readableBytes());
            // Add keep alive header as per:
            // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
            response.setHeader(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        // Write the response.
        ChannelFuture future = e.getChannel().write(response);

        // Close the non-keep-alive connection after the write operation is done.
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static void send100Continue(MessageEvent e) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, CONTINUE);
        e.getChannel().write(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        e.getCause().printStackTrace();
        e.getChannel().close();
    }
}