package io.goudai.dubbo.restful.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.goudai.dubbo.restful.container.MetaCache;
import io.goudai.dubbo.restful.exception.ServiceNotFonudException;
import io.goudai.dubbo.restful.model.Response;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by freeman on 16/6/15.
 */
public class JettyRpcHandler extends AbstractHandler {
	Logger logger = LoggerFactory.getLogger(JettyRpcHandler.class);

	//	userService -> <login,Method(login)>
	public static final Map<String, Map<String, MetaCache>> metaCacheMap = new HashMap<>(64);
	public static final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		response.setContentType("application/json;charset=utf-8");
		final Response res = new Response();
		Object data = null;
		try {
			if (!"application/json".equals(request.getContentType())) {
				throw new IllegalArgumentException("request content type must be application/json,your request content type is [" + request.getContentType() + "]");
			}

			/* Finding service and method for cache */
			MetaCache metaCache = findMeta(request);

			JsonNode node = getBody(request);

			Map<String, Class<?>> arguments = metaCache.getArguments();
			/* Method called with no arguments */
			if (arguments.size() == 0) {
				data = metaCache.invoke();
			}
			/* Method called with arguments*/
			else {
				Object[] parseArguments = parseArguments(node, arguments);
				data = metaCache.invoke(parseArguments);
			}

			res.setData(data);
			response.setStatus(HttpServletResponse.SC_OK);
		} catch (Throwable e) {
			if (e instanceof java.lang.reflect.InvocationTargetException) {
				e = ((InvocationTargetException) e).getTargetException();
			}
			logger.error(e.getMessage(), e);
			res.setErrorType(e.getClass().getName());
			res.setError(e);
			res.setSuccess(false);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		} finally {
			baseRequest.setHandled(true);
			String s = objectMapper.writeValueAsString(res);
			response.getWriter().write(s);
		}
	}

	/**
	 * parse body to java arguments
	 *
	 * @param node
	 * @param arguments
	 * @return
	 */
	private Object[] parseArguments(JsonNode node, Map<String, Class<?>> arguments) throws ParseException, IOException {
		Object[] args = new Object[arguments.size()];
		int i = 0;
		for (Map.Entry<String, Class<?>> entry : arguments.entrySet()) {
			final String key = entry.getKey();
			JsonNode value = node.path(key);
			Class<?> type = entry.getValue();
			if (value.isMissingNode()) {
				throw new IllegalArgumentException("Parameter [" + key + "] must be exists");
			}
			if (value.isValueNode()) {
				args[i] = getBasicValue(key, value, type);
			} else if (type.isEnum()) {
				if (value.isNull())
					args[i] = null;
				if (value.isIntegralNumber()) {
					Enum[] enumConstants = ((Class<? extends Enum>) type).getEnumConstants();
					for (Enum enumConstant : enumConstants) {
						int ordinal = enumConstant.ordinal();
						if (ordinal == value.intValue()) {
							args[i] = enumConstant;
						}
					}
				} else if (value.isTextual()) {
					args[i] = Enum.valueOf((Class<? extends Enum>) type, value.textValue());

				}
				throw new IllegalArgumentException("Parameter [" + key + "] must be string or long or null");
			} else if (type.isArray() || Collection.class.isAssignableFrom(type)) {
				if (value.isNull())
					args[i] = null;
				if (!value.isArray()) {
					throw new IllegalArgumentException("Parameter [" + key + "] must be array or null");
				}
				if (type.isArray()) {
					args[i] = objectMapper.readValue(value.toString(), objectMapper.getTypeFactory().constructArrayType(type.getComponentType()));
				} else {
					Class<?> aClass = (Class<?>) ((ParameterizedType) type.getGenericSuperclass()).getActualTypeArguments()[0];
					args[i] = objectMapper.readValue(value.toString(), objectMapper.getTypeFactory().constructCollectionType((Class<? extends Collection>) type, aClass));

				}
			} else if (value.isObject()) {
				if (value.isNull())
					args[i] = null;
				args[i] = objectMapper.readValue(value.toString(), type);
			} else {
				throw new IllegalArgumentException("Parameter [" + key + "] not support");
			}
			i++;
		}
		return args;
	}

	private Object getBasicValue(String key, JsonNode value, Class<?> type) throws ParseException {
		if (String.class.isAssignableFrom(type)) {
			return value.asText();
		}
		if (int.class.isAssignableFrom(type)) {
			if (value.isNull())
				throw new NullPointerException("Parameter [" + key + "] value must not be null");
			if (!value.isIntegralNumber())
				throw new IllegalArgumentException("Parameter [" + key + "] value must be int");
			return value.intValue();
		} else if (Integer.class.isAssignableFrom(type)) {
			if (value.isNull())
				return null;
			else if (value.isIntegralNumber()) {
				return value.intValue();
			} else {
				throw new IllegalArgumentException("Parameter [" + key + "] value  must be integer or null");
			}
		} else if (float.class.isAssignableFrom(type)) {
			if (value.isNull())
				throw new NullPointerException("Parameter [" + key + "] value must not be null");
			if (!value.isNumber())
				throw new IllegalArgumentException("Parameter [" + key + "] value must be float");
			return value.floatValue();
		} else if (Float.class.isAssignableFrom(type)) {
			if (value.isNull())
				return null;
			else if (value.isNumber()) {
				return value.floatValue();
			}
			throw new IllegalArgumentException("Parameter [" + key + "] value  must be Float or null");

		} else if (double.class.isAssignableFrom(type)) {
			if (value.isNull())
				throw new NullPointerException("Parameter [" + key + "] value must not be null");
			if (!value.isNumber())
				throw new IllegalArgumentException("Parameter [" + key + "] value must be double");
			return value.doubleValue();
		} else if (Double.class.isAssignableFrom(type)) {
			if (value.isNull())
				return null;
			else if (value.isNumber()) {
				return value.doubleValue();
			} else {
				throw new IllegalArgumentException("Parameter [" + key + "] value  must be Double or null");
			}
		} else if (long.class.isAssignableFrom(type)) {
			if (value.isNull())
				throw new NullPointerException("Parameter [" + key + "] value must not be null");
			if (!value.isNumber())
				throw new IllegalArgumentException("Parameter [" + key + "] value must be long");
			return value.longValue();

		} else if (Long.class.isAssignableFrom(type)) {
			if (value.isNull())
				return null;
			else if (value.isNumber()) {
				return value.longValue();
			}else if(value.isTextual()){
				return Long.parseLong(value.asText());
			}
			throw new IllegalArgumentException("Parameter [" + key + "] value  must be Long or null");
		} else if (Date.class.isAssignableFrom(type)) {
			if (value.isNull())
				return null;
			else if (value.isTextual()) {
				String text = value.textValue();
				if (text.length() == "yyyy-MM-dd".length()) {
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
					return dateFormat.parse(text);
				} else if (text.length() == "yyyy-MM-dd HH:mm:ss".length()) {
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					return dateFormat.parse(text);
				}
			} else if (value.isNumber()) {
				return new Date(value.longValue());
			}
			throw new IllegalArgumentException("Parameter [" + key + "] must be string or long or null");
		}
		throw new IllegalArgumentException("Parameter [" + key + "] connet parse");
	}


	private JsonNode getBody(HttpServletRequest request) throws IOException {
		//TODO
		if (request.getMethod().equals("GET")) {
			String body = request.getParameter("body");
			if (body == null || "".equals(body))
				throw new NullPointerException("body must not be null");
			return objectMapper.readValue(body, JsonNode.class);
		}
		return objectMapper.readValue(request.getInputStream(), JsonNode.class);
	}


	private MetaCache findMeta(HttpServletRequest request) throws ServiceNotFonudException {
		String service = request.getParameter("service");
		if (service == null || "".equals(service))
			throw new NullPointerException("service name  must not be null");

		String method = request.getParameter("method");
		if (method == null)
			throw new NullPointerException("method name must not be null");

		Map<String, MetaCache> stringMetaCacheMap = metaCacheMap.get(service);
		if (stringMetaCacheMap == null)
			throw new ServiceNotFonudException("service : [" + service + "] not found provider ");

		MetaCache metaCache = stringMetaCacheMap.get(method);
		if (metaCache == null) {
			throw new ServiceNotFonudException("service name [" + service + "] method name [" + method + "] cannot found");
		}
		return metaCache;

	}

}

