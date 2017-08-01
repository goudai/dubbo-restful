package io.goudai;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Unit test for simple .
 */
public class AppTest

{

	@Test
	public void testJetty() throws Exception {
		int threads = 300;
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setDaemon(true);
		threadPool.setMaxThreads(threads);
		threadPool.setMinThreads(threads);
		Server server = new Server(threadPool);
		ServerConnector serverConnector = new ServerConnector(server);
		serverConnector.setPort(8080);
		server.setConnectors(new Connector[]{serverConnector});
		NCSARequestLog ncsaRequestLog = new NCSARequestLog();
		RequestLogHandler requestLogHandler = new RequestLogHandler();
		requestLogHandler.setRequestLog(ncsaRequestLog);
		HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[]{
				requestLogHandler,
				new AbstractHandler() {
					@Override
					public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
						response.setContentType("text/html;charset=utf-8");
						response.setStatus(HttpServletResponse.SC_OK);
						baseRequest.setHandled(true);
						response.getWriter().println("<h1>Hello World2</h1>");
					}
				},
				new GzipHandler()


		});

		server.setHandler(handlers);
		server.start();
		server.join();
	}


	@Test
	public  void testMapper() throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNode = objectMapper.readValue("{\"id\":1,\"user\":{\"id\":1,\"name\":\"yi\"}}", JsonNode.class);
		JsonNode id = jsonNode.path("id");


		JsonNode user = jsonNode.path("user");
//        Integer integer = objectMapper.readValue("{\"id\":1}",);
//        System.out.println(integer);
		String x = id.asText();
		System.out.println(x);
		System.out.println(user);
	}

	@Test
	public void testJsonNode() throws IOException {
		String json = "{\"age\":\"2.13355555\",\"name\":{\"last\":\"Hankcs\",\"first\":\"Joe\"},\"userImage\":\"Rm9vYmFyIQ==\",\"gender\":\"MALE\"}";

		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNode = objectMapper.readValue(json, JsonNode.class);
		JsonNode name = jsonNode.path("age");
		System.out.println(name.getClass());


	}

	@Test
	public void testInvoker() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Object o = Long.valueOf(1);
		Object user = new User();
		Object[] objects = {o,user};
		Object test = User.class.getMethod("test", Long.class, User.class).invoke(new User(), objects);
		System.out.println(test);

	}

	@Test
	public void testException2Json() throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		NullPointerException value = new NullPointerException("key be not null");
		String s1 = value.toString();
		System.out.println(s1);
		String s = objectMapper.writeValueAsString(value);
		System.out.println(s);
	}
	@Test
	public void testDateformat() throws ParseException {
		Date parse = new SimpleDateFormat("yyyy-MM-dd").parse("2016-01-02");
		DateFormat dateInstance = DateFormat.getDateInstance();
		String format = dateInstance.format(parse);
		System.out.println(format);
	}
	@Test
	public void testJson2Array() throws IOException {
		String content = "{\"id\":\"[1,2,3,4,5,6]\"}";
		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonNode = mapper.readValue(content, JsonNode.class);
		JsonNode id = jsonNode.path("id");
		System.out.println(id.asText());
		System.out.println(id.toString());
		String content1 = id.textValue();
		System.out.println(content1);
		System.out.println(id.toString());

//		Collection collection = mapper.readValue(content1, mapper.getTypeFactory().constructCollectionType(List.class, int.class));
//		System.out.println(collection.getClass());
	}

	@Test
	public void testMethodHanle(){

	}

}

class User {
	String last, first;


	public String getLast() {
		return last;
	}

	public void setLast(String last) {
		this.last = last;
	}

	public String getFirst() {
		return first;
	}

	public void setFirst(String first) {
		this.first = first;
	}

	public void test(Long id, User user) {
		System.out.println(id + "" + user);
	}
}
