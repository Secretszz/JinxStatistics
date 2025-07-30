package com.jinx.statistics.dao;

import com.jinx.statistics.pojo.Statistics;
import com.jinx.statistics.utility.FileUtility;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.Thread.sleep;

@Component
@Slf4j
public class StatisticsDao {

    /**
     * 应用属性
     */
    @Value("${app.statistics-file-dir}")
    String fileDir;

    /**
     * 当前的缓存
     */
    final Map<String, Statistics> cache;

    /**
     * 当前日期（文件夹）
     */
    String date;

    /**
     * 需要压缩的文件夹缓存
     */
    List<String> zipCache;

    public StatisticsDao() {
        cache = new HashMap<>();
        date = nowStrYMD();
        zipCache = new ArrayList<>();
    }

    public Statistics appendStatistics(String name, String value) {
        String key = String.format("%s_%s", date, name);
        Statistics obj = cache.get(key);
        if (obj == null) {
            obj = new Statistics(date, name, value, this.fileDir);
            cache.put(key, obj);
        } else {
            obj.append(value);
        }
        return obj;
    }

    public File[] getFiles(String dirName){
        File dir = FileUtility.getDirectory(String.join("/", this.fileDir, dirName));
        return dir.listFiles();
    }

    public File getFile(String path) throws Exception {
        path = String.join("/", this.fileDir, path);
        return FileUtility.getFile(path);
    }

    private String nowStrYMD(){
        return (new SimpleDateFormat("yyyyMMdd")).format(new Date());
    }

    /**
     * 定时写入新的日志
     */
    @Scheduled(initialDelayString = "${app.scheduled}", fixedRateString = "${app.scheduled}")
    public void scheduledUpdate(){
        try {
            saveCaches();
            checkNextDate();
        } catch (Exception e) {
            log.error("upload statistics error!", e);
        }
    }

    @PreDestroy
    public void preShutdown(){
        try {
            saveCaches();
        } catch (Exception e) {
            log.error("upload statistics error!", e);
        }
    }

    private void saveCaches() throws Exception {
        if (cache.isEmpty()) {
            log.info("cache is empty");
            return;
        }
        // 缓存需要上传的统计数据
        List<Statistics> objs = new ArrayList<>(cache.values());
        // 清除缓存
        cache.clear();
        for (Statistics obj : objs) {
            obj.saveFile();
            sleep(2);
        }
    }

    private void checkNextDate() {
        String nowDate = nowStrYMD();
        if (Objects.equals(date, nowDate)) {
            if (zipCache.isEmpty()){
                return; // 待压缩文件夹列表为空，跳出判断
            }
            // 开始压缩文件夹
            for (String dirName : zipCache) {
                zipFile(dirName);
            }
        } else {
            // 到了第二天，将需要压缩的文件夹名缓存下来，等待下次执行
            zipCache.add(date);
            date = nowDate;
        }
    }

    public String zipFile(String dir) {
        try {
            if (dir.equals(date)){
                return "today file can not be zipped";
            }
            String filePath = String.join("/", this.fileDir, dir);
            File file = FileUtility.openFile(filePath);
            if (!file.exists()) {
                log.info("zip file {} not exists", dir);
                return "file not exists";
            }
            if (file.isHidden()){
                log.info("zip file {} is hidden", dir);
                return "file is hidden";
            }

            String zipFilePath = String.format("%s/%s.zip", this.fileDir, file.getName());
            FileUtility.zipFolder(filePath, zipFilePath);
            log.info("文件压缩成功: {} ", zipFilePath);
            return "zip success";
        } catch (Exception e) {
            log.error("zip file error!", e);
            return "zip file error! " + e.getMessage();
        }
    }
}
