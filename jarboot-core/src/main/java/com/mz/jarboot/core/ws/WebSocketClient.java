package com.mz.jarboot.core.ws;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;

public final class WebSocketClient {
    private URI uri;
    private EventLoopGroup group;
    private Channel channel;
    public WebSocketClient(String url) {
        try {
            this.uri = new URI(url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void sendText(String text) {
        if (null != channel) {
            channel.writeAndFlush(text);
        }
    }

    public static void main(String[] args) throws Exception {
        WebSocketClient client = new WebSocketClient("ws://127.0.0.1:9899/jarboot-service/ws");
        client.connect();
        client.sendText("你好");
        System.in.read();
        client.disconnect();
    }
    public void connect() {
        String scheme = uri.getScheme() == null ? "ws" : uri.getScheme();
        final String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
        final int port;
        if (uri.getPort() == -1) {
            if ("ws".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("wss".equalsIgnoreCase(scheme)) {
                port = 443;
            } else {
                port = -1;
            }
        } else {
            port = uri.getPort();
        }

        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            System.err.println("Only WS(S) is supported.");
            return;
        }

        final boolean ssl = "wss".equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        if (ssl) {
            try {
                sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            } catch (SSLException e) {
                return;
            }
        } else {
            sslCtx = null;
        }

        group = new NioEventLoopGroup();
        try {
            final WebSocketClientHandler handler = new WebSocketClientHandler(WebSocketClientHandshakerFactory
                    .newHandshaker(uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders()));

            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline p = ch.pipeline();
                    if (sslCtx != null) {
                        p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                    }
                    p.addLast(new HttpClientCodec(), new HttpObjectAggregator(8192), handler);
                }
            });

            channel = b.connect(uri.getHost(), port).sync().channel();
            handler.handshakeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        this.group.shutdownGracefully();
    }
}
