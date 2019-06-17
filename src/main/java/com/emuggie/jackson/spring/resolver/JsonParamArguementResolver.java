package com.emuggie.jackson.spring.resolver;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.emuggie.jackson.spring.annotation.JsonParam;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JsonParam annotated method parameter resolver. 
 * MultiRead HTTP request wrapper Required.
 * TODO : handle with single read resolver (selfish mode).
 * @author emuggie
 *
 */
public class JsonParamArguementResolver implements HandlerMethodArgumentResolver {
	
	private ObjectMapper mapper;
	
	private static final Logger logger = LoggerFactory.getLogger(JsonParamArguementResolver.class);
	
	public JsonParamArguementResolver(ObjectMapper mapper){
		this.mapper = mapper;
	}
	
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(JsonParam.class);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		
		JsonParam annotation = parameter.getParameterAnnotation(JsonParam.class);
		String name = "".equals(annotation.name().trim()) ? parameter.getParameterName() : annotation.name().trim();
		
		logger.debug("Resolve JsonParam for :" + name);
		if(!(webRequest instanceof ServletWebRequest)) {
			logger.debug(
					String.format("Fail casting %s to %s"
							, webRequest.getClass().getCanonicalName()
							, ServletWebRequest.class.getCanonicalName()
					)
			);
			return null;
		}
		ServletWebRequest req = (ServletWebRequest)webRequest;
		logger.debug("Request Method : "+ req.getHttpMethod());
		
		Class<?> targetClass = parameter.getParameterType();
		String jsonStr = null;
		if(Stream.of(new HttpMethod[]{HttpMethod.POST,HttpMethod.PUT})
			.anyMatch(each -> each.equals(req.getHttpMethod()))) {
			HttpServletRequest httpReq = req.getRequest(); 
			logger.debug("ContentType : "+ httpReq.getContentType());
			String reqBody = httpReq.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
			logger.debug("Body : "+ reqBody);
			
			// Parse body when POST/PUT method
			if(httpReq.getContentType().contains(MediaType.APPLICATION_JSON_VALUE)) {
				JsonNode root = this.mapper.readTree(reqBody);
				jsonStr = root.get(name) == null ? null : root.get(name).asText();
			}else if(httpReq.getContentType().contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
				jsonStr =  req.getParameter(name);
			}
		}else {
			// Parse parameter with other methods.
			jsonStr =  req.getParameter(name);
		}
		Object result = this.mapper.convertValue(jsonStr, targetClass);
		logger.debug("Resolved : "+ result);
		return result;
	}

}
