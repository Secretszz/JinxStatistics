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
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

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
     * @param logDTO 日志数据
     */
    @Override
    public void log(StatisticsLogDTO logDTO) {
        String name = logDTO.getName();
        String value = logDTO.getValue();
        if (!StringUtils.hasLength(name)) {
            throw new BaseException(MessageConstant.EMPTY_NAME);
        }
        if (!StringUtils.hasLength(value)){
            throw new BaseException(MessageConstant.EMPTY_VALUE);
        }

        dao.appendStatistics(name, value);
    }

    /**
     * 获取文件夹内文件列表
     *
     * @param dirName 文件夹名
     * @return 用于网页显示的文本内容
     */
    @Override
    public String list(String dirName) {
        try {
            String refDownload = String.format("/statistics/download/%s", dirName);
            String refList = "/statistics/list";

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
                    StringUtility.appendLine(sb, "<li><a href=\"%s\">%s</a></li>", new Object[]{refDownload + "/", "下载zip全部文件"});
                }
            }
            for (File file : files) {
                boolean isDir = file.isDirectory();
                String name = file.getName();
                if (!isDir && !name.endsWith(".csv")) {
                    continue;
                }
                String ref = String.join("/", (isDir ? refList : refDownload), name);
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
     * @param dir  文件夹名
     * @param file 文件名
     * @return 文件
     */
    @Override
    public Resource download(String dir, String file) {
        try {
            String filePath = "backups/statistics/" + dir;
            if (StringUtils.hasLength(file)) {
                filePath = String.format("%s/%s", filePath, file);
            } else {
                filePath = filePath + ".zip";
            }
            Resource resource = new ClassPathResource(filePath);
            if (!resource.exists()) {
                throw new BaseException(MessageConstant.EMPTY_FILE);
            }
            return resource;
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
