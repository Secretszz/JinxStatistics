package com.jinx.statistics;

import com.jinx.statistics.properties.AppProperties;
import com.jinx.statistics.utility.DateUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;

import java.util.Date;

@SpringBootApplication
@Slf4j
public class JinxStatisticsApplication {

    static final Runtime runtime = Runtime.getRuntime();
    static ApplicationContext applicationContext;

    public static void main(String[] args) {
        applicationContext = SpringApplication.run(JinxStatisticsApplication.class, args);

        try {
            long startTime = DateUtility.now();
            String startDate = DateUtility.format(new Date());
            long freeMemory = runtime.freeMemory();
            logSuccess(startTime, startDate, freeMemory);
        } catch (Exception e) {
            logFailure(e);
        }
    }

    private static void logSuccess(long startTime, String startDate, long freeMemory){
        long freeMemory2 = runtime.freeMemory();
        long totalMemory = runtime.totalMemory();
        String endDate = DateUtility.format(new Date());
        long df = DateUtility.now() - startTime;
        String strStarUp = df + " 毫秒(ms)";
        StringBuffer sb = new StringBuffer();
        String end = "\r\n";
        sb.append(end);
        sb.append("/////////////////////////////////////////").append(end);
        sb.append(String.format("// Application   :%s v: %.2f", AppProperties.APP_NAME, AppProperties.VERSION)).append(end);
        sb.append(String.format("// WEB_PORT on   :%s", AppProperties.WEB_PORT)).append(end);
        sb.append(String.format("// Used Memory   :%s", (freeMemory - freeMemory2) / 1048576L + "MB")).append(end);
        sb.append(String.format("// Free Memory   :%s", freeMemory2 / 1048576L + "MB")).append(end);
        sb.append(String.format("// Total Memory  :%s", totalMemory / 1048576L + "MB")).append(end);
        sb.append(String.format("// Start Time    :%s", startDate)).append(end);
        sb.append(String.format("// End Time      :%s", endDate)).append(end);
        sb.append(String.format("// Use Time      :%s", strStarUp)).append(end);
        sb.append("/////////////////////////////////////////").append(end);
        log.info(sb.toString());
    }

    private static void logFailure(Exception e){
        StringBuffer sb = new StringBuffer();
        String end = "\r\n";
        sb.append("/////////////////////////////////////////").append(end);
        sb.append(String.format("// Application   :%s v: %.2f", AppProperties.APP_NAME, AppProperties.VERSION)).append(end);
        sb.append(String.format("// WEB_PORT on   :%s", AppProperties.WEB_PORT)).append(end);
        sb.append(String.format("// End Time      :%s", DateUtility.format(new Date()))).append(end);
        sb.append(String.format("// Exception     :%s", e.getMessage())).append(end);
        sb.append("/////////////////////////////////////////").append(end);
        log.info(sb.toString());

        SpringApplication.exit(applicationContext, () -> 0);
    }
}
