package com.jinx.statistics.interceptor;

import com.jinx.statistics.response.ApiResponse;
import com.jinx.statistics.utility.GsonUtility;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 统计拦截器
 * 用于请求日志记录、错误处理和性能监控
 */
@Component
@Slf4j
public class StatisticsInterceptor implements HandlerInterceptor {
    
    /**
     * 是否启用详细日志
     */
    @Value("${logging.request.verbose:false}")
    private boolean verboseLogging;
    
    /**
     * 请求计数器
     */
    private final AtomicInteger requestCounter = new AtomicInteger(0);
    
    /**
     * 请求处理前的拦截方法
     * 
     * @param request HTTP请求
     * @param response HTTP响应
     * @param handler 处理器对象
     * @return 是否继续执行后续处理
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 增加请求计数
        int currentCount = requestCounter.incrementAndGet();
        
        // 记录请求开始时间
        request.setAttribute("startTime", System.currentTimeMillis());
        
        // 获取请求信息
        String requestId = String.format("REQ-%d", currentCount);
        String method = request.getMethod();
        String url = request.getRequestURL().toString();
        String queryString = request.getQueryString();
        String fullUrl = queryString != null ? url + "?" + queryString : url;
        
        // 设置请求ID，便于跟踪
        request.setAttribute("requestId", requestId);
        
        // 判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            // 当前拦截到的不是动态方法，直接放行
            if (verboseLogging) {
                log.info("[{}] 静态资源请求: {} {}", requestId, method, fullUrl);
            }
            return true;
        }
        
        // 记录API请求日志
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        String controllerName = handlerMethod.getBeanType().getSimpleName();
        String methodName = handlerMethod.getMethod().getName();
        
        log.info("[{}] 接收请求: {} {} ({}#{})", 
                requestId, method, fullUrl, controllerName, methodName);
        
        // 错误路径处理
        if (url.contains("error")) {
            log.warn("[{}] 检测到错误路径访问: {}", requestId, fullUrl);
            handleErrorResponse(response, "检测到错误路径访问", requestId);
            return false;
        }
        
        // 放行
        return true;
    }
    
    /**
     * 请求处理后的拦截方法
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        if (verboseLogging) {
            String requestId = (String) request.getAttribute("requestId");
            log.debug("[{}] 请求处理完成，视图渲染前", requestId);
        }
    }
    
    /**
     * 请求完全处理完毕后的拦截方法
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 获取请求ID
        String requestId = (String) request.getAttribute("requestId");
        
        // 计算请求处理时间
        Long startTime = (Long) request.getAttribute("startTime");
        if (startTime != null) {
            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;
            
            // 获取请求信息
            String method = request.getMethod();
            String url = request.getRequestURL().toString();
            int status = response.getStatus();
            
            // 记录请求完成日志
            if (ex != null) {
                log.error("[{}] 请求异常: {} {} - 状态码:{} - 耗时:{}ms - 异常:{}", 
                        requestId, method, url, status, processingTime, ex.getMessage());
            } else {
                log.info("[{}] 请求完成: {} {} - 状态码:{} - 耗时:{}ms", 
                        requestId, method, url, status, processingTime);
            }
        }
    }
    
    /**
     * 处理错误响应
     * 
     * @param response HTTP响应对象
     * @param errorMessage 错误消息
     * @param requestId 请求ID
     */
    private void handleErrorResponse(HttpServletResponse response, String errorMessage, String requestId) throws IOException {
        // 设置响应状态和内容类型
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        // 创建错误响应
        ApiResponse<String> error = ApiResponse.error(errorMessage);
        
        // 写入响应
        response.getWriter().write(GsonUtility.toJson(error));
    }
}
