package org.k0D3St0rY.cs2013.server;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import org.jboss.netty.handler.codec.http.multipart.DiskFileUpload;
import org.jboss.netty.handler.codec.http.multipart.FileUpload;
import org.jboss.netty.handler.codec.http.multipart.HttpDataFactory;
import org.jboss.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import org.jboss.netty.handler.codec.http.multipart.HttpPostRequestDecoder.EndOfDataDecoderException;
import org.jboss.netty.handler.codec.http.multipart.HttpPostRequestDecoder.ErrorDataDecoderException;
import org.jboss.netty.handler.codec.http.multipart.HttpPostRequestDecoder.IncompatibleDataDecoderException;
import org.jboss.netty.handler.codec.http.multipart.HttpPostRequestDecoder.NotEnoughDataDecoderException;
import org.jboss.netty.handler.codec.http.multipart.InterfaceHttpData;
import org.jboss.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.util.CharsetUtil;
import org.k0D3St0rY.cs2013.service.ServiceFactory;

import com.google.common.base.Charsets;
import com.google.inject.Inject;

public class HttpCSHandler extends SimpleChannelHandler {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(HttpCSHandler.class);

    private final ServiceFactory serviceFactory;

    private HttpRequest request;

    private boolean readingChunks;

    private static final HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE); // Disk if
                                                                                                               // MINSIZE

    private HttpPostRequestDecoder decoder;
    static {
        DiskFileUpload.deleteOnExitTemporaryFile = true; // should delete file
                                                         // on exit (in normal
                                                         // exit)
        DiskFileUpload.baseDirectory = null; // system temp directory
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

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (!readingChunks) {
            // clean previous FileUpload if Any
            if (decoder != null) {
                decoder.cleanFiles();
                decoder = null;
            }
            HttpRequest request = this.request = (HttpRequest) e.getMessage();
            logger.info("## " + request.getUri());
            URI uri = new URI(request.getUri());

            if (uri.getPath().startsWith("/")) {
                QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
                Map<String, List<String>> params = queryStringDecoder.getParameters();
                if (params.size() > 0) {
                    CharSequence result = serviceFactory.create("/").execute(params);
                    logger.info("## " + request.getUri() + " >> " + result);
                    HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                    response.setContent(ChannelBuffers.copiedBuffer(result, Charsets.UTF_8));
                    e.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
                }
            }

            if (!uri.getPath().startsWith("/enonce")) {
                return;
            }

            // if GET Method: should not try to create a HttpPostRequestDecoder
            try {
                decoder = new HttpPostRequestDecoder(factory, request);
                logger.info("## DECODER " + decoder);
            } catch (ErrorDataDecoderException e1) {
                e1.printStackTrace();
                writeResponse(e.getChannel());
                Channels.close(e.getChannel());
                return;
            } catch (IncompatibleDataDecoderException e1) {
                writeResponse(e.getChannel());
                return;
            }

            if (request.isChunked()) {
                // Chunk version
                readingChunks = true;
            } else {
                // Not chunk version
                readHttpDataAllReceive(e.getChannel());
                writeResponse(e.getChannel());
            }
        } else {
            logger.info("## DECODER " + decoder);
            // New chunk is received
            HttpChunk chunk = (HttpChunk) e.getMessage();
            try {
                decoder.offer(chunk);
            } catch (ErrorDataDecoderException e1) {
                e1.printStackTrace();
                writeResponse(e.getChannel());
                Channels.close(e.getChannel());
                return;
            }
            readHttpDataChunkByChunk(e.getChannel());
            if (chunk.isLast()) {
                readHttpDataAllReceive(e.getChannel());
                writeResponse(e.getChannel());
                readingChunks = false;
            }
        }
    }

    /**
     * Example of reading all InterfaceHttpData from finished transfer
     * 
     * @param channel
     */
    private void readHttpDataAllReceive(Channel channel) {
        logger.info("readHttpDataAllReceive");
        List<InterfaceHttpData> datas = null;
        try {
            datas = decoder.getBodyHttpDatas();
            logger.info("readHttpDataAllReceive datas : " + datas);
        } catch (NotEnoughDataDecoderException e1) {
            // Should not be!
            e1.printStackTrace();
            writeResponse(channel);
            Channels.close(channel);
            return;
        }

        logger.info("readHttpDataAllReceive data size : " + datas.size());
        for (InterfaceHttpData data : datas) {
            writeHttpData(data);
        }
    }

    private void readHttpDataChunkByChunk(Channel channel) {
        logger.info("readHttpDataAllReceive");
        try {
            while (decoder.hasNext()) {
                InterfaceHttpData data = decoder.next();
                if (data != null) {
                    // new value
                    writeHttpData(data);
                }
            }
        } catch (EndOfDataDecoderException e1) {
        }
    }

    private void writeHttpData(InterfaceHttpData data) {
        logger.info("writeHttpData");
        if (data.getHttpDataType() == HttpDataType.FileUpload) {
            FileUpload fileUpload = (FileUpload) data;
            if (fileUpload.isCompleted()) {
                if (fileUpload.length() < 10000) {
                    try {
                        logger.info("## Post : \n" + fileUpload.getString(fileUpload.getCharset()));
                    } catch (IOException e1) {
                        // do nothing for the example
                        e1.printStackTrace();
                    }
                } else {
                    logger.warn("File too long :" + fileUpload.length());
                }
            }
        }
    }

    private void writeResponse(Channel channel) {

        // Decide whether to close the connection or not.
        boolean close = HttpHeaders.Values.CLOSE.equalsIgnoreCase(request.getHeader(HttpHeaders.Names.CONNECTION))
                || request.getProtocolVersion().equals(HttpVersion.HTTP_1_0)
                && !HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase(request.getHeader(HttpHeaders.Names.CONNECTION));

        // Build the response object.
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CREATED);
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");

        if (!close) {
          response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, "0");
        }

        // Write the response.
        ChannelFuture future = channel.write(response);
        if (close) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        logger.error("exceptionCaught", e.getCause());
        e.getChannel().close();
    }
}
