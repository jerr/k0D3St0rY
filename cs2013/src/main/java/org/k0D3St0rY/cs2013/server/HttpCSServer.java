package org.k0D3St0rY.cs2013.server;

import gnu.getopt.Getopt;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.socket.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class HttpCSServer {

    private final String bind;
    private final int port;

    public HttpCSServer(String bind, int port) {
        this.bind = bind;
        this.port = port;
    }

    public void run() throws Exception {
        System.out.println("Strarting server! (" + bind + ":" + port +")");
        // Configure the server.
        HttpCSServerBootstrap boot = new HttpCSServerBootstrap(bind).localAddress(bind, port);
        try {
            boot.group(new NioEventLoopGroup(), new NioEventLoopGroup()).channel(NioServerSocketChannel.class)
                    .childHandler(new HttpCSServerInitializer()).localAddress(new InetSocketAddress(port));

            Channel ch = boot.bind().sync().channel();
            ch.closeFuture().sync();
        } finally {
            boot.shutdown();
        }
    }

    public static void main(String[] args) {
        try {
            String bind = InetAddress.getLocalHost().getHostAddress();
            int port = 8080;
            Getopt g = new Getopt(HttpCSServer.class.getSimpleName().toLowerCase(), args, "b:p:");
            int c;
            while ((c = g.getopt()) != -1) {
                switch (c) {
                    case 'b':
                        bind = g.getOptarg();
                        break;
                    case 'p':
                        port = Integer.parseInt(g.getOptarg());
                        break;
                }
            }
            new HttpCSServer(bind, port).run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}