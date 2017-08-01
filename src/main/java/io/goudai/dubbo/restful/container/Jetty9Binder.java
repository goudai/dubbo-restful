package io.goudai.dubbo.restful.container;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.HttpServer;

/**
 * Created by freeman on 16/6/14.
 */
public class Jetty9Binder implements HttpBinder {


	@Override
	public HttpServer bind(URL url, HttpHandler handler) {
		return new Jetty9HttpServer(url, handler);
	}


}
