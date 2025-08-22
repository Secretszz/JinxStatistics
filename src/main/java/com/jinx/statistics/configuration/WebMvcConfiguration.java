package com.jinx.statistics.configuration;

import com.jinx.statistics.interceptor.StatisticsInterceptor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.util.Arrays;
import java.util.List;

/**
 * Web MVC 配置类
 * 负责注册web层相关组件、配置拦截器、静态资源和跨域策略
 */
@Configuration
@Slf4j
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    /**
     * 应用名称
     */
    @Value("${spring.application.name}")
    private String applicationName;
    
    /**
     * API版本
     */
    @Value("${spring.application.version}")
    private String apiVersion;
    
    /**
     * 是否启用跨域
     */
    @Value("${app.cors.enabled}")
    private boolean corsEnabled;
    
    /**
     * 统计拦截器
     */
    private final StatisticsInterceptor statisticsInterceptor;

    /**
     * 构造函数，注入依赖
     * @param statisticsInterceptor 统计拦截器
     */
    public WebMvcConfiguration(StatisticsInterceptor statisticsInterceptor) {
        this.statisticsInterceptor = statisticsInterceptor;
    }

    /**
     * 配置OpenAPI文档
     * 通过knife4j生成接口文档
     * @return OpenAPI配置
     */
    @Bean
    public OpenAPI customOpenAPI() {
        log.info("正在初始化API文档配置...");
        
        // 创建联系人信息
        Contact contact = new Contact()
                .name("Jinx团队")
                .email("sungame0824@163.com")
                .url("https://www.sunnygame666.com");
        
        // 创建许可证信息
        License license = new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0.html");
        
        // 创建API信息
        Info info = new Info()
                .title(applicationName + "接口文档")
                .version(apiVersion)
                .description("提供数据统计、文件管理等功能的API接口")
                .contact(contact)
                .license(license);
        
        // 创建服务器信息
        List<Server> servers = Arrays.asList(
                new Server().url("/").description("本地服务器")
        );
        
        // 返回OpenAPI配置
        return new OpenAPI()
                .info(info)
                .servers(servers);
    }

    /**
     * 添加拦截器
     * @param registry 拦截器注册表
     */
    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        log.info("注册统计拦截器...");
        registry.addInterceptor(this.statisticsInterceptor)
                .addPathPatterns("/**")  // 拦截所有请求
                .excludePathPatterns(    // 排除不需要拦截的路径
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/doc.html"
                );
    }
    
    /**
     * 配置静态资源处理
     * @param registry 资源处理器注册表
     */
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("配置静态资源处理...");
        // 配置knife4j的静态资源路径
        registry.addResourceHandler("doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
                
        // 添加自定义静态资源路径
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
                
        super.addResourceHandlers(registry);
    }
    
    /**
     * 配置跨域请求处理
     * @param registry 跨域注册表
     */
    @Override
    protected void addCorsMappings(CorsRegistry registry) {
        if (corsEnabled) {
            log.info("启用跨域支持...");
            registry.addMapping("/**")  // 所有接口
                    .allowedOriginPatterns("*")  // 允许所有来源
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // 允许的HTTP方法
                    .allowedHeaders("*")  // 允许所有头
                    .allowCredentials(true)  // 允许发送cookie
                    .maxAge(3600);  // 预检请求的有效期，单位为秒
        }
        super.addCorsMappings(registry);
    }
}
