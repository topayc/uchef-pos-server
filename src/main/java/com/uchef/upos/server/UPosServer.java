package com.uchef.upos.server;

import org.apache.log4j.Logger;

import com.uchef.upos.backgroud.BackgroundService;
import com.uchef.upos.config.Config;
import com.uchef.upos.config.Configuration;
import com.uchef.upos.handler.initializer.UPOSServerInitializer;
import com.uchef.upos.manager.IServer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class UPosServer implements IServer {

	private final static Logger logger = Logger.getLogger(UPosServer.class);
	
	private BackgroundService backgroundService; 
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;

	public void init() {}

	public void start() {
		try {
			Configuration.getInstance().initialize();
			bossGroup = new NioEventLoopGroup(1);
			workerGroup = new NioEventLoopGroup();

			ServerBootstrap serverBootStrap = new ServerBootstrap();
			serverBootStrap.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.handler(new LoggingHandler(LogLevel.INFO))
				.childHandler(new UPOSServerInitializer())
				.option(ChannelOption.SO_KEEPALIVE, true)
				.option(ChannelOption.SO_REUSEADDR, true)
				.option(ChannelOption.SO_BACKLOG, 50)
				.option(ChannelOption.SO_LINGER, 0);
			
			ChannelFuture future = serverBootStrap.bind(Configuration.getInstance().getInt(Config.LISTEN_PORT)).sync();
			logger.info("UPOS Server started ");
			startBackgroundProcessor();
			
			
			future.channel().closeFuture().sync();
		} catch (Exception e) {
			logger.error("Fail in starting server caused by");
			logger.error(e.getMessage());
			logger.error(e.getStackTrace());
			stop();
		}
	}

	public void stop() {
		stopBackgroundProcessor();
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
		logger.info("UPOS Server terminated.....");
	}
	
	private void startBackgroundProcessor() {
		backgroundService = new BackgroundService();
		backgroundService.start();
	}
	
	public void stopBackgroundProcessor(){
		if (this.backgroundService != null)
			this.backgroundService.stop();
	}


}
