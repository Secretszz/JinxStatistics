package com.jinx.statistics.properties;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    public static String APP_NAME;
    public static Integer WEB_PORT;
    public static double VERSION;

    private String name;
    private Long scheduled;
    private Integer webPort;
    private double ver;
    private String fileDir;

    @PostConstruct
    public void init() {
        AppProperties.APP_NAME = this.name;
        AppProperties.WEB_PORT = this.webPort;
        AppProperties.VERSION = this.ver;
    }
}
