package io.github.sweetpark.apm.test.config;

import io.github.sweetpark.apm.core.context.TraceContextHolder;
import io.github.sweetpark.apm.core.enums.TraceLevel;
import io.github.sweetpark.apm.core.sql.SqlTraceContext;
import io.github.sweetpark.apm.core.sql.SqlTraceContextHolder;
import io.github.sweetpark.apm.support.netty.NettyTraceDuplexHandler;
import io.github.sweetpark.apm.test.mapper.TestMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.AttributeKey;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.sql.SQLException;

@Configuration
public class TestNettyConfig {

    private final NettyTraceDuplexHandler nettyTraceDuplexHandler;
    private final TestMapper testMapper;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private int port;

    public TestNettyConfig(NettyTraceDuplexHandler nettyTraceDuplexHandler, TestMapper testMapper) {
        this.nettyTraceDuplexHandler = nettyTraceDuplexHandler;
        this.testMapper = testMapper;
    }

    public int getPort() {
        return port;
    }

    @PostConstruct
    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast("logging_head", nettyTraceDuplexHandler);
                        ch.pipeline().addLast(new StringDecoder());
                        ch.pipeline().addLast(new StringEncoder());
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            private final StringBuilder accum = new StringBuilder();

                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                String traceId = ctx.channel().attr(AttributeKey.<String>valueOf("APM_TRACE_ID")).get();
                                String spanId = ctx.channel().attr(AttributeKey.<String>valueOf("APM_SPAN_ID")).get();
                                SqlTraceContext sqlCtx = ctx.channel().attr(AttributeKey.<SqlTraceContext>valueOf("APM_SQL_CONTEXT")).get();

                                if (traceId != null) {
                                    TraceContextHolder.init(traceId, spanId, TraceLevel.TRACE, true);
                                    SqlTraceContextHolder.set(sqlCtx);
                                }

                                try {
                                    String s = (String) msg;
                                    accum.append(s);
                                    if (s.contains("\n")) {
                                        String finalMsg = accum.toString().trim();
                                        if ("ERROR_TEST".equals(finalMsg)) {
                                            testMapper.selectParam("Before Error");
                                            throw new SQLException("Netty Test Error");
                                        } else {
                                            testMapper.selectParam("Netty-" + finalMsg);
                                            ctx.writeAndFlush("Echo: " + finalMsg + "\n");
                                        }
                                        accum.setLength(0);
                                    }
                                } finally {
                                    TraceContextHolder.clear();
                                    SqlTraceContextHolder.clear();
                                }
                            }
                        });
                        ch.pipeline().addLast("logging_tail", nettyTraceDuplexHandler);
                    }
                });

        ChannelFuture f = b.bind(0).sync();
        this.port = ((InetSocketAddress) f.channel().localAddress()).getPort();
    }

    @PreDestroy
    public void stop() {
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
    }
}