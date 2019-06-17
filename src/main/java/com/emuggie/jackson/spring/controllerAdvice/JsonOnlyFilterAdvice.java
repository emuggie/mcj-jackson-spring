package com.emuggie.jackson.spring.controllerAdvice;

import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.emuggie.mcj.jackson.annotation.JsonOnly;
import com.emuggie.mcj.jackson.filter.JsonOnlyPropertyFilter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ControllerAdvice for JsonOnlyFilterAdvice. 
 * Purpose is to acquire annotated filter rule for method return type.
 * @author emuggie
 *
 */
public class JsonOnlyFilterAdvice implements ResponseBodyAdvice<Object>, ApplicationContextAware{

	private static final Logger logger = LoggerFactory.getLogger(JsonOnlyFilterAdvice.class);
	
	private ObjectMapper mapper;
	
	/**
	 * Default implementation : Use default object mapper given by spring.
	 */
	public JsonOnlyFilterAdvice() {
		
	}
	
	/**
	 * Init with configured object mapper to serialize.
	 * @param mapper
	 */
	public JsonOnlyFilterAdvice(ObjectMapper mapper) {
		this();
		this.mapper = mapper;
	}
	
	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return returnType.getMethod().isAnnotationPresent(JsonOnly.class);
	}

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
			Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
			ServerHttpResponse response) {
		JsonOnly methodAnnotation = returnType.getMethod().getAnnotation(JsonOnly.class);
		logger.debug(methodAnnotation.toString());
		
		Object target = body;
		boolean isResponseEntity = false;
		if(body instanceof ResponseEntity) {
			target = ((ResponseEntity<?>)body).getBody();
			isResponseEntity = true;
		}
		
		if(body == null || target == null) {
			return body;
		}
		
		JsonOnlyPropertyFilter filter = (JsonOnlyPropertyFilter) this.mapper.getSerializerProviderInstance().getFilterProvider().findPropertyFilter(
				JsonOnlyPropertyFilter.FILTER_ID, target);
		
		filter.addTemporaryRule(target, methodAnnotation);
		
		// If method return type present and type is not annotated(escapes filter)
		if(!returnType.getMethod().getReturnType().isAnnotationPresent(JsonOnly.class)) {
			logger.debug("Wrapper applied");
			if(isResponseEntity) {
				ResponseEntity<?> rawEntity = (ResponseEntity<?>)body;
				return new ResponseEntity<Unwrapper>( 
						new Unwrapper().setValue(target)
						, rawEntity.getHeaders()
						, rawEntity.getStatusCode()); 
			}
			return new Unwrapper().setValue(target);
		}
		
		return body;
	}
	
	/**
	 * Wrapper class forcing to invoke JsonFilter.
	 * @author emuggie
	 *
	 */
	public class Unwrapper {
		@JsonUnwrapped
		@JsonOnly({})
		public Object value;
		public Unwrapper setValue(Object value) {
			this.value = value;
			return this;
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		// Ignore if mapper is already assigned.
		if(this.mapper != null){
			return;
		}
		// Map first found ObjectMapper bean to mapper.
		// If multiple bean or no bean found, log error.
		try {
			Map<String,ObjectMapper> map = applicationContext.getBeansOfType(ObjectMapper.class);
			if(map.size() == 0) {
				logger.error("Bean not found for type : " + ObjectMapper.class.getCanonicalName());
				return;
			}
			if(map.size() > 1) {
				logger.error("Multiple Bean found for type : " + ObjectMapper.class.getCanonicalName());
			}
			Entry<String,ObjectMapper> e = map.entrySet().iterator().next();
			logger.info(String.format(
					"Filter will use ObjectMapper bean with name[%s]:[%s]"
					, e.getKey()
					, e.getValue()));
		}catch(Throwable e) {
			logger.error("Look up default object bean failed.\n" + e.getMessage());
		}
	}
}
