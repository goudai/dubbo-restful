package io.goudai.dubbo.restful.container;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.support.AbstractHttpServer;
import io.goudai.dubbo.restful.protocol.JettyRpcHandler;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by freeman on 16/6/14.
 */
public class Jetty9HttpServer extends AbstractHttpServer {

	final static Logger logger = LoggerFactory.getLogger(Jetty9HttpServer.class);

	private Server server;


	public Jetty9HttpServer(URL url, HttpHandler handler) {
		super(url, handler);
		int threads = url.getParameter(Constants.THREADS_KEY, Constants.DEFAULT_THREADS);
		logger.info("init jetty runing threads size [{}]",threads);
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setDaemon(true);
		threadPool.setMaxThreads(threads);
		threadPool.setMinThreads(threads);

		server = new Server(threadPool);
		ServerConnector serverConnector = new ServerConnector(server);
		int port = url.getPort(8080);
		logger.info("dubbo jetty bind port [{}]",port);
		serverConnector.setPort(port);
		server.setConnectors(new Connector[]{serverConnector});
		HandlerList handlers = new HandlerList();

		RequestLog ncsaRequestLog = new Slf4jRequestLog();
		RequestLogHandler requestLogHandler = new RequestLogHandler();
		requestLogHandler.setRequestLog(ncsaRequestLog);

		handlers.setHandlers(new Handler[]{
				requestLogHandler,
				new JettyRpcHandler()
		});

		server.setHandler(handlers);
		try {
			server.start();
			logger.info("Dubbo jetty rest plugin started  success");
		} catch (Exception e) {
			throw new IllegalStateException(
					"Failed to start jetty9 server on " + url.getAddress() + ", cause: " + e.getMessage(), e);
		}


	}


	@Override
	public void close() {
		try {
			this.server.stop();
		} catch (Exception e) {
			//ig
		}
	}

	@Override
	public boolean isClosed() {
		return this.server.isStopped();
	}
}


