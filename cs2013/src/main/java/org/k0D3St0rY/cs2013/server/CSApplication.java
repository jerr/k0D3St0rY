package org.k0D3St0rY.cs2013.server;

import gnu.getopt.Getopt;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.k0D3St0rY.cs2013.service.AbstractCSService;
import org.k0D3St0rY.cs2013.service.CSQuestionService;
import org.k0D3St0rY.cs2013.service.ServiceFactory;
import org.k0D3St0rY.cs2013.service.ServiceFactoryImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class CSApplication extends AbstractModule {

    private final String addr;
    private final int port;
    private final CSQuestionService questionService = new CSQuestionService();

    public CSApplication(String addr, int port) {
        this.addr = addr;
        this.port = port;
    }

    @Override
    protected void configure() {
    }

    @Provides
    SocketAddress provideSocketAddress() {
        return new InetSocketAddress(addr, port);
    }

    @Provides
    @Singleton
    ChannelGroup provideChannelGroup() {
        return new DefaultChannelGroup("http-server");
    }

    @Provides
    @Singleton
    ServiceFactory provideServiceFactory() {
        return new ServiceFactoryImpl();
    }

    @Provides
    AbstractCSService provideService(String path) {
        if ("ROOT".equals(path))
            return questionService;
        else
            return null;

    }

    @Provides
    ChannelFactory provideChannelFactory() {
        return new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
    }

    public static void main(String[] args) {
        try {
            String addr = InetAddress.getLocalHost().getHostAddress();
            int port = 8080;
            Getopt g = new Getopt(CSApplication.class.getSimpleName().toLowerCase(), args, "b:p:");
            int c;
            while ((c = g.getopt()) != -1) {
                switch (c) {
                    case 'b':
                        addr = g.getOptarg();
                        break;
                    case 'p':
                        port = Integer.parseInt(g.getOptarg());
                        break;
                }
            }

            Injector injector = Guice.createInjector(new CSApplication(addr, port));
            
            final HttpCSServer server = injector.getInstance(HttpCSServer.class);
            server.startAndWait();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    server.stopAndWait();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
