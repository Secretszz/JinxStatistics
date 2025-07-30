package com.jinx.statistics.interceptor;

import com.jinx.statistics.response.ApiResponse;
import com.jinx.statistics.utility.GsonUtility;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 拦截器，用于拦截一些系统性错误
 */
@Component
@Slf4j
public class StatisticsInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }

        //1.获取请求url。
        String url = request.getRequestURL().toString();
        log.info("请求的url: {}",url);

        if (url.contains("error")){
            ApiResponse error = ApiResponse.error("error");
            response.getWriter().write(GsonUtility.toJson(error));
            return false;
        }

        // 放行
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        log.info("postHandle...");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        log.info("afterCompletion...");
    }
}
