package br.com.erp.shared;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
/** Exact decimal JSON values for workflow map responses; internal calculations retain BigDecimal. */
@RestControllerAdvice
public class WorkflowAmountAdvice implements ResponseBodyAdvice<Object> {
 public boolean supports(MethodParameter method,Class<? extends HttpMessageConverter<?>> converter){return method.getContainingClass().isAnnotationPresent(WorkflowAmounts.class);}
 public Object beforeBodyWrite(Object body,MethodParameter method,MediaType media,Class<? extends HttpMessageConverter<?>> converter,ServerHttpRequest request,ServerHttpResponse response){return encode(body);}
 private Object encode(Object value){if(value instanceof BigDecimal n)return n.toPlainString();if(value instanceof Map<?,?> map){var out=new LinkedHashMap<String,Object>();map.forEach((k,v)->out.put(k.toString(),encode(v)));return out;}if(value instanceof List<?> list)return list.stream().map(this::encode).toList();return value;}
}
