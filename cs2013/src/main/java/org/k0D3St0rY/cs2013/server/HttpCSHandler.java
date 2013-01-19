package org.k0D3St0rY.cs2013.server;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

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
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunk;
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
import org.jboss.netty.handler.codec.http.multipart.InterfaceHttpData;
import org.jboss.netty.handler.codec.http.multipart.HttpPostRequestDecoder.EndOfDataDecoderException;
import org.jboss.netty.handler.codec.http.multipart.HttpPostRequestDecoder.ErrorDataDecoderException;
import org.jboss.netty.handler.codec.http.multipart.HttpPostRequestDecoder.IncompatibleDataDecoderException;
import org.jboss.netty.handler.codec.http.multipart.HttpPostRequestDecoder.NotEnoughDataDecoderException;
import org.jboss.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.k0D3St0rY.cs2013.service.ServiceFactory;

import com.google.common.base.Charsets;
import com.google.inject.Inject;

public class HttpCSHandler extends SimpleChannelHandler {
    private final ChannelGroup allChannels;
    private final ServiceFactory serviceFactory;

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(HttpCSHandler.class);

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
        this.allChannels = allChannels;
        this.serviceFactory = serviceFactory;
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        allChannels.add(e.getChannel());
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

            HttpRequest request = (HttpRequest) e.getMessage();
            logger.info("## " + request.getUri());
            URI uri = new URI(request.getUri());

            if (uri.getPath().startsWith("/?q")) {
                QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
                Map<String, List<String>> params = queryStringDecoder.getParameters();
                if (params.size() > 0) {
                    CharSequence result = serviceFactory.create("/").execute(params);
                    logger.info("## " + request.getUri() + " >> " + result);
                    HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                    response.setContent(ChannelBuffers.copiedBuffer(result, Charsets.UTF_8));
                    e.getChannel().write(response).addListener(new ChannelFutureListener() {
                        public void operationComplete(ChannelFuture future) throws Exception {
                            future.getChannel().close();
                        }
                    });
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
                Channels.close(e.getChannel());
                return;
            } catch (IncompatibleDataDecoderException e1) {
                return;
            }

            if (request.isChunked()) {
                // Chunk version
                readingChunks = true;
            } else {
                // Not chunk version
                readHttpDataAllReceive(e.getChannel());
            }
        } else {
            // New chunk is received
            HttpChunk chunk = (HttpChunk) e.getMessage();
            try {
                decoder.offer(chunk);
            } catch (ErrorDataDecoderException e1) {
                e1.printStackTrace();
                Channels.close(e.getChannel());
                return;
            }
            readHttpDataChunkByChunk(e.getChannel());
            if (chunk.isLast()) {
                readHttpDataAllReceive(e.getChannel());
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
            Channels.close(channel);
            return;
        }

        logger.info("readHttpDataAllReceive data size : " + datas.size());
        for (InterfaceHttpData data : datas) {
            writeHttpData(data);
        }
    }

    /**
     * Example of reading request by chunk and getting values from chunk to chunk
     * 
     * @param channel
     */
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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        logger.error("exceptionCaught", e.getCause());
        e.getChannel().close();
    }
}
