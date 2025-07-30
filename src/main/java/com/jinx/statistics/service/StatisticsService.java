package com.jinx.statistics.service;

import java.io.File;
import java.util.List;

public interface StatisticsService {
    /**
     * 统计日志
     * @param names 数据表名列表
     * @param values 数据表值列表
     */
    void log(List<String> names, List<String> values);

    /**
     * 获取src/main/resources/backups/statistics/文件夹内文件列表
     * @param dir 文件夹名
     * @return 用于网页显示的文本内容
     */
    String list(String dir);

    /**
     * 下载src/main/resources/backups/statistics/文件夹下的文件
     * @param path 文件路径
     * @return 文件
     */
    File download(String path);

    /**
     * 压缩src/main/resources/backups/statistics/文件夹下的文件夹
     * @param dir 文件夹名
     */
    String zip(String dir);
}
