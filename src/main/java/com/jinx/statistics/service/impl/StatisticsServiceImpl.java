package com.jinx.statistics.service.impl;

import com.jinx.statistics.constant.MessageConstant;
import com.jinx.statistics.dao.StatisticsDao;
import com.jinx.statistics.exception.BaseException;
import com.jinx.statistics.pojo.dto.StatisticsLogDTO;
import com.jinx.statistics.service.StatisticsService;
import com.jinx.statistics.utility.FileUtility;
import com.jinx.statistics.utility.StringUtility;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@EnableScheduling
public class StatisticsServiceImpl implements StatisticsService {

    private final StatisticsDao dao;

    public StatisticsServiceImpl(StatisticsDao dao) {
        this.dao = dao;
    }

    /**
     * 统计日志
     *
     * @param logDTOs 日志数据
     */
    @Override
    public void log(List<StatisticsLogDTO> logDTOs) {
        if (logDTOs == null || logDTOs.isEmpty()) {
            return;
        }
        for (StatisticsLogDTO logDTO : logDTOs) {
            String name = logDTO.getName();
            String value = logDTO.getValue();
            if (!StringUtils.hasLength(name) || !StringUtils.hasLength(value)) {
                continue;
            }

            dao.appendStatistics(name, value);
        }
    }

    /**
     * 获取文件夹内文件列表
     *
     * @param dirName 文件夹名
     * @return 用于网页显示的文本内容
     */
    @Override
    public String list(String dirName) {
        if (dirName == null) {
            dirName = "";
        }
        try {
            String refDownload = "/statistics/download?path=";
            String refList = "/statistics/list?path=";

            Object[] empty = new Object[0];
            File[] files = dao.getFiles(dirName);
            int length = files.length;

            StringBuffer sb = new StringBuffer();
            StringUtility.appendLine(sb, "<h1>Download file</h1>", empty);
            StringUtility.appendLine(sb, "<div>file list count: %s</div>", new Object[]{length});
            StringUtility.appendLine(sb, "<ul>", empty);
            if (StringUtils.hasLength(dirName)) {
                StringUtility.appendLine(sb, "<li><a href=\"%s\">..</a></li>", new Object[]{refList});
                if (!dirName.equals(nowStrYMD())){
                    StringUtility.appendLine(sb, "<li><a href=\"%s\">%s</a></li>", new Object[]{refDownload.concat(dirName + ".zip"), "下载zip全部文件"});
                }
            }
            for (File file : files) {
                boolean isDir = file.isDirectory();
                String name = file.getName();
                String ref;
                if (isDir){
                    ref = refList.concat(name);
                } else {
                    if (StringUtils.hasLength(dirName)) {
                        ref = refDownload.concat(String.join("/", dirName, name));
                    } else {
                        ref = refDownload.concat(name);
                    }
                }
                StringUtility.appendLine(sb, "<li><a href=\"%s\">%s</a></li>", new Object[]{ref, name});
            }
            StringUtility.appendLine(sb, "</ul>", empty);
            return listHtml(sb.toString());
        } catch (Exception e) {
            throw new BaseException(e);
        }
    }

    /**
     * 下载文件
     *
     * @param path  文件夹名
     * @return 文件
     */
    @Override
    public File download(String path) {
        try {
            File file = dao.getFile(path);
            if (!file.exists()) {
                throw new BaseException(MessageConstant.EMPTY_FILE);
            }
            return file;
        } catch (Exception e) {
            throw new BaseException(e);
        }
    }

    /**
     * 压缩src/main/resources/backups/statistics/文件夹下的文件夹
     *
     * @param dir 文件夹名
     */
    @Override
    public String zip(String dir) {
        return dao.zipFile(dir);
    }

    private String nowStrYMD(){
        return (new SimpleDateFormat("yyyyMMdd")).format(new Date());
    }

    private String listHtml(String bodyContent) throws Exception {
        File file = FileUtility.getFile("src/main/resources/templates/StatisticsList.html");
        byte[] bytes = Files.readAllBytes(file.toPath());
        String template = new String(bytes, StandardCharsets.UTF_8);

        Document document = Jsoup.parse(template);

        Element body = document.body();
        body.html(""); // 清空body
        body.append(bodyContent);
        return document.outerHtml();
    }
}
