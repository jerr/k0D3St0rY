package org.k0D3St0rY.cs2013.server;

import gnu.getopt.Getopt;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public class HttpCSServer {

    private final String bind;
    private final int port;

    public HttpCSServer(String bind, int port) {
        this.bind = bind;
        this.port = port;
    }

    public void run() throws Exception {
        System.out.println("Strarting server! (" + bind + ":" + port + ")");

        // Configure the server.
        ServerBootstrap bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()));

        // Set up the event pipeline factory.
        bootstrap.setPipelineFactory(new HttpCSServerPipelineFactory());

        // Bind and start to accept incoming connections.
        bootstrap.bind(new InetSocketAddress(bind, port));
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