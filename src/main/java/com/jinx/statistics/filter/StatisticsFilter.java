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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * IP过滤器
 * 用于过滤非法IP请求，只允许配置文件中指定的IP地址访问系统
 */
@Slf4j
@WebFilter
@Component
public class StatisticsFilter implements Filter {

    /**
     * IP许可列表
     * 使用ConcurrentHashMap的keySet作为线程安全的Set实现
     */
    private final Set<String> allowedIps = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * 配置文件最后修改时间
     * 使用AtomicLong确保线程安全的更新
     */
    private final AtomicLong lastModifiedTime = new AtomicLong(0);

    /**
     * IP配置文件的读写锁
     */
    private final ReadWriteLock configFileLock = new ReentrantReadWriteLock();

    /**
     * IP配置文件
     */
    private volatile File ipsConfigFile;

    /**
     * IP配置文件路径
     */
    @Value("${app.ips-config-path:config/ips.xml}")
    private String filePath;

    /**
     * 是否启用IP过滤
     */
    @Value("${app.ip-filter.enabled:true}")
    private boolean ipFilterEnabled;

    /**
     * 是否允许本地回环地址
     */
    @Value("${app.ip-filter.allow-localhost:true}")
    private boolean allowLocalhost;

    /**
     * 过滤器初始化
     * 加载IP配置文件并初始化允许的IP列表
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("初始化IP过滤器...");
        
        // 初始化时加载IP配置
        try {
            loadIpConfig();
            log.info("IP过滤器初始化完成，已加载{}个允许的IP地址", allowedIps.size());
        } catch (Exception e) {
            log.error("初始化IP过滤器失败", e);
            // 如果初始化失败，至少允许本地访问
            if (allowLocalhost) {
                allowedIps.add("127.0.0.1");
                allowedIps.add("0:0:0:0:0:0:0:1"); // IPv6 localhost
                log.info("已添加本地回环地址到允许列表");
            }
        }
    }

    /**
     * 过滤器销毁
     */
    @Override
    public void destroy() {
        log.info("IP过滤器已销毁");
    }

    /**
     * 执行过滤
     * 检查请求IP是否在允许列表中
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        // 如果未启用IP过滤，直接放行
        if (!ipFilterEnabled) {
            chain.doFilter(request, response);
            return;
        }
        
        // 获取请求IP地址
        String ipAddress = getClientIpAddress(request);
        
        // 检查IP是否在允许列表中
        if (allowedIps.contains(ipAddress)) {
            if (log.isDebugEnabled()) {
                log.debug("IP地址[{}]验证通过，请求放行", ipAddress);
            }
            chain.doFilter(request, response);
        } else {
            // IP不在允许列表中，拒绝请求
            log.warn("拒绝来自非法IP[{}]的请求", ipAddress);
            handleUnauthorizedAccess(response, ipAddress);
        }
    }

    /**
     * 获取客户端真实IP地址
     * 考虑代理服务器转发的情况
     */
    private String getClientIpAddress(ServletRequest request) {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            
            // 尝试从代理头获取真实IP
            String ip = httpRequest.getHeader("X-Forwarded-For");
            
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = httpRequest.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = httpRequest.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = httpRequest.getHeader("HTTP_CLIENT_IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = httpRequest.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = httpRequest.getRemoteAddr();
            }
            
            // 如果是多个IP，取第一个
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            
            return ip;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 处理未授权访问
     */
    private void handleUnauthorizedAccess(ServletResponse response, String ipAddress) throws IOException {
        if (response instanceof HttpServletResponse) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        }
        
        ApiResponse<String> error = ApiResponse.error(MessageConstant.ILLEGAL_IP_ADDRESS);
        error.setData("IP: " + ipAddress);
        response.getWriter().write(GsonUtility.toJson(error));
    }

    /**
     * 定时检查IP配置文件是否被修改
     * 如果有更新，重新加载配置
     */
    @Scheduled(fixedDelayString = "${app.ip-filter.check-interval:5000}")
    public void checkForChanges() {
        try {
            // 检查配置文件是否存在
            configFileLock.readLock().lock();
            try {
                if (ipsConfigFile == null) {
                    configFileLock.readLock().unlock();
                    loadIpConfig();
                    configFileLock.readLock().lock();
                }
                
                // 检查文件是否被修改
                long currentModified = ipsConfigFile.lastModified();
                if (currentModified > lastModifiedTime.get()) {
                    // 文件已更新，重新加载配置
                    configFileLock.readLock().unlock();
                    loadIpConfig();
                    return;
                }
            } finally {
                configFileLock.readLock().unlock();
            }
        } catch (Exception e) {
            log.error("检查IP配置文件更新失败", e);
        }
    }
    
    /**
     * 加载IP配置文件
     */
    private void loadIpConfig() throws Exception {
        configFileLock.writeLock().lock();
        try {
            // 获取或创建配置文件
            ipsConfigFile = FileUtility.getFile(filePath);
            lastModifiedTime.set(ipsConfigFile.lastModified());
            
            log.info("正在加载IP配置文件: {}", ipsConfigFile.getAbsolutePath());
            
            // 创建新的IP列表
            Set<String> newAllowedIps = Collections.newSetFromMap(new ConcurrentHashMap<>());
            
            // 始终允许本地回环地址（如果配置允许）
            if (allowLocalhost) {
                newAllowedIps.add("127.0.0.1");
                newAllowedIps.add("0:0:0:0:0:0:0:1"); // IPv6 localhost
            }
            
            // 解析XML配置文件
            String content = new String(Files.readAllBytes(ipsConfigFile.toPath()), StandardCharsets.UTF_8);
            JSONObject jsonObject = XmlParserUtils.parseXml(content);
            
            if (jsonObject != null && jsonObject.containsKey("ips")) {
                JSONObject ipsObj = jsonObject.getJSONObject("ips");
                if (ipsObj != null && ipsObj.containsKey("ip")) {
                    JSONArray ips = ipsObj.getJSONArray("ip");
                    for (int i = 0; i < ips.size(); i++) {
                        String ip = ips.getString(i);
                        if (ip != null && !ip.isEmpty()) {
                            newAllowedIps.add(ip.trim());
                        }
                    }
                }
            }
            
            // 更新允许的IP列表
            allowedIps.clear();
            allowedIps.addAll(newAllowedIps);
            
            log.info("IP配置已更新，当前允许{}个IP地址访问", allowedIps.size());
            if (log.isDebugEnabled()) {
                log.debug("允许的IP列表: {}", allowedIps);
            }
        } finally {
            configFileLock.writeLock().unlock();
        }
    }
}
