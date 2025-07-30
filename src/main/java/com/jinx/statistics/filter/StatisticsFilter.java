package com.jinx.statistics.filter;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jinx.statistics.constant.MessageConstant;
import com.jinx.statistics.response.ApiResponse;
import com.jinx.statistics.utility.FileUtility;
import com.jinx.statistics.utility.GsonUtility;
import com.jinx.statistics.utility.XmlParserUtils;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;


/**
 * 过滤器，用于过滤一些非法请求
 */
@Slf4j
@WebFilter
@Component
public class StatisticsFilter implements Filter {

    /**
     * ip许可列表
     */
    private List<String> allowedIps;

    /**
     * 间隔多久检查一次ip许可列表配置是否更新了
     */
    private long lastModifiedTime;

    private File ipsConfigFile;

    @Value("${app.ips-config-path}")
    private String filePath;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("[ {} ] 创建啦...", this.getClass().getSimpleName());
    }

    @Override
    public void destroy() {
        log.info("[ {} ] 被摧毁啦...", this.getClass().getSimpleName());
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        // 1、查看是否是允许的ip地址
        String ipAddress = servletRequest.getRemoteAddr();
        if (!allowedIps.contains(ipAddress)) {
            ApiResponse<String> error = ApiResponse.error(MessageConstant.ILLEGAL_IP_ADDRESS);
            servletResponse.getWriter().write(GsonUtility.toJson(error));
            return;
        }

        log.info("[ {} ] ip地址合法，放行...", ipAddress);
        filterChain.doFilter(servletRequest, servletResponse);
    }

    /**
     * 定时检查文件是否被修改
     */
    @Scheduled(fixedDelay = 5000)
    public void checkForChanges(){
        try {
            if (ipsConfigFile == null) {
                ipsConfigFile = FileUtility.getFile(filePath);
            }
            //Resource resource = resourceLoader.getResource("classpath:ips/IpsConfig.xml");
            if (ipsConfigFile.lastModified() > lastModifiedTime) {
                lastModifiedTime = ipsConfigFile.lastModified();
                log.info("Resource has changed, refreshing...");
                // 执行资源刷新逻辑
                allowedIps = new ArrayList<>();
                allowedIps.add("127.0.0.1");
                // 1、加载并解析xml文件
                try {
                    String content = new String(Files.readAllBytes(ipsConfigFile.toPath()));
                    JSONObject jsonObject = XmlParserUtils.parseXml(content);
                    JSONArray ips = jsonObject.getJSONObject("ips").getJSONArray("ip");
                    for (int i = 0; i < ips.size(); i++) {
                        allowedIps.add(ips.getString(i));
                    }
                    log.info("allowedIps: {}", allowedIps);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
