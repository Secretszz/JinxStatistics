package com.jinx.statistics.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * 配置类，注册web层相关组件
 */
@Configuration
@Slf4j
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    /**
     * 通过knife4j生成接口文档
     * @return
     */
    @Bean
    public OpenAPI customOpenAPI() {
        log.info("准备生成管理端接口文档...");
        Info info = new Info()
                .title("数据统计接口文档")
                .version("1.0")
                .description("数据统计接口文档");
        return new OpenAPI()
                .info(info);
    }
}
