package com.jinx.statistics.service;

import com.jinx.statistics.pojo.dto.StatisticsLogDTO;
import org.springframework.core.io.Resource;

public interface StatisticsService {
    /**
     * 统计日志
     * @param logDTO 日志数据
     */
    void log(StatisticsLogDTO logDTO);

    /**
     * 获取src/main/resources/backups/statistics/文件夹内文件列表
     * @param dir 文件夹名
     * @return 用于网页显示的文本内容
     */
    String list(String dir);

    /**
     * 下载src/main/resources/backups/statistics/文件夹下的文件
     * @param dir 文件夹名
     * @param file 文件名
     * @return 文件
     */
    Resource download(String dir, String file);

    /**
     * 压缩src/main/resources/backups/statistics/文件夹下的文件夹
     * @param dir 文件夹名
     */
    String zip(String dir);
}
