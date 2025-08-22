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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
@Slf4j
public class StatisticsDao {

    /**
     * 应用属性
     */
    @Value("${app.statistics-file-dir}")
    String fileDir;

    /**
     * 缓存配置参数
     */
    @Value("${app.cache.max-size}")
    private int maxCacheSize;

    @Value("${app.cache.flush-threshold}")
    private int flushThreshold;
    
    /**
     * 当前的缓存 - 使用线程安全的ConcurrentHashMap
     */
    private final ConcurrentHashMap<String, Statistics> cache;
    
    /**
     * 缓存读写锁 - 用于批量操作时的线程安全
     */
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
    
    /**
     * 缓存统计
     */
    private final AtomicInteger cacheHits = new AtomicInteger(0);
    private final AtomicInteger cacheMisses = new AtomicInteger(0);
    private final AtomicInteger cacheSize = new AtomicInteger(0);

    /**
     * 当前日期（文件夹）
     */
    private volatile String date;

    /**
     * 需要压缩的文件夹缓存
     */
    private final List<String> zipCache;

    public StatisticsDao() {
        cache = new ConcurrentHashMap<>();
        date = nowStrYMD();
        zipCache = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * 添加统计数据到缓存
     * @param name 统计名称
     * @param value 统计值
     * @return 统计对象
     */
    public Statistics appendStatistics(String name, String value) {
        String key = String.format("%s_%s", date, name);
        
        // 尝试从缓存获取
        Statistics obj = cache.get(key);
        
        if (obj == null) {
            // 缓存未命中
            cacheMisses.incrementAndGet();
            
            // 检查缓存大小是否超过限制
            if (cacheSize.get() >= maxCacheSize) {
                // 如果缓存已满，尝试刷新部分缓存到磁盘
                try {
                    flushOldestEntries(maxCacheSize / 4); // 刷新25%的缓存
                } catch (Exception e) {
                    log.error("刷新缓存失败", e);
                }
            }
            
            // 创建新的统计对象
            obj = new Statistics(date, name, value, this.fileDir);
            Statistics oldObj = cache.putIfAbsent(key, obj);
            
            // 处理并发情况下的冲突
            if (oldObj != null) {
                oldObj.append(value);
                return oldObj;
            } else {
                cacheSize.incrementAndGet();
            }
        } else {
            // 缓存命中
            cacheHits.incrementAndGet();
            obj.append(value);
            
            // 如果单个统计对象的数据量过大，考虑立即刷新到磁盘
            if (cacheSize.get() > flushThreshold) {
                try {
                    obj.saveFile();
                } catch (Exception e) {
                    log.error("保存单个统计对象失败", e);
                }
            }
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
     * 刷新最旧的n个缓存条目到磁盘
     * @param count 要刷新的条目数量
     */
    private void flushOldestEntries(int count) throws Exception {
        if (cache.isEmpty()) {
            return;
        }
        
        cacheLock.writeLock().lock();
        try {
            // 按照键排序，通常日期较早的会排在前面
            List<Map.Entry<String, Statistics>> entries = new ArrayList<>(cache.entrySet());
            int flushCount = Math.min(count, entries.size());
            
            for (int i = 0; i < flushCount; i++) {
                Map.Entry<String, Statistics> entry = entries.get(i);
                Statistics obj = entry.getValue();
                obj.saveFile();
                cache.remove(entry.getKey());
                cacheSize.decrementAndGet();
            }
            
            log.info("已刷新{}个缓存条目到磁盘", flushCount);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * 获取缓存统计信息
     * @return 缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", cacheSize.get());
        stats.put("maxCacheSize", maxCacheSize);
        stats.put("cacheHits", cacheHits.get());
        stats.put("cacheMisses", cacheMisses.get());
        
        // 计算命中率
        int totalRequests = cacheHits.get() + cacheMisses.get();
        double hitRatio = totalRequests > 0 ? (double) cacheHits.get() / totalRequests : 0;
        stats.put("hitRatio", String.format("%.2f%%", hitRatio * 100));
        
        return stats;
    }

    /**
     * 定时写入新的日志
     */
    @Scheduled(initialDelayString = "${app.scheduled}", fixedRateString = "${app.scheduled}")
    public void scheduledUpdate(){
        try {
            saveCaches();
            checkNextDate();
            // 记录缓存统计信息
            if (log.isDebugEnabled()) {
                Map<String, Object> stats = getCacheStats();
                log.debug("缓存统计: {}", stats);
            }
        } catch (Exception e) {
            log.error("上传统计数据错误!", e);
        }
    }

    @PreDestroy
    public void preShutdown(){
        try {
            log.info("应用关闭前保存所有缓存数据");
            saveCaches();
        } catch (Exception e) {
            log.error("关闭前保存缓存数据错误!", e);
        }
    }

    private void saveCaches() throws Exception {
        if (cache.isEmpty()) {
            //log.info("缓存为空");
            return;
        }
        
        cacheLock.writeLock().lock();
        try {
            // 缓存需要上传的统计数据
            List<Statistics> objs = new ArrayList<>(cache.values());
            log.info("准备保存{}个缓存对象到磁盘", objs.size());
            
            // 清除缓存
            cache.clear();
            cacheSize.set(0);
            
            // 批量保存文件，使用并行流提高性能
            objs.parallelStream().forEach(obj -> {
                try {
                    obj.saveFile();
                } catch (Exception e) {
                    log.error("保存统计对象失败: {}", e.getMessage());
                }
            });
            
            log.info("所有缓存对象已保存到磁盘");
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * 检查日期变更并处理文件压缩
     */
    private void checkNextDate() {
        String nowDate = nowStrYMD();
        
        if (Objects.equals(date, nowDate)) {
            if (zipCache.isEmpty()){
                return; // 待压缩文件夹列表为空，跳出判断
            }
            
            // 创建一个副本以避免并发修改异常
            List<String> dirNamesToZip;
            synchronized (zipCache) {
                dirNamesToZip = new ArrayList<>(zipCache);
                zipCache.clear();
            }
            
            // 异步处理压缩任务，避免阻塞主线程
            CompletableFuture.runAsync(() -> {
                for (String dirName : dirNamesToZip) {
                    try {
                        zipFile(dirName);
                        log.info("成功压缩文件夹: {}", dirName);
                    } catch (Exception e) {
                        log.error("压缩文件夹失败: {}", dirName, e);
                        // 如果压缩失败，重新添加到压缩队列
                        synchronized (zipCache) {
                            zipCache.add(dirName);
                        }
                    }
                }
            }).exceptionally(ex -> {
                log.error("异步压缩任务失败", ex);
                return null;
            });
        } else {
            // 到了第二天，将需要压缩的文件夹名缓存下来，等待下次执行
            synchronized (zipCache) {
                zipCache.add(date);
            }
            date = nowDate;
            log.info("日期已更新为: {}, 旧日期文件夹已加入压缩队列", nowDate);
        }
    }

    /**
     * 压缩指定文件夹
     * @param dir 文件夹名称
     * @return 压缩结果描述
     */
    public String zipFile(String dir) {
        // 检查是否为当前日期文件夹
        if (dir.equals(date)){
            log.warn("当前日期的文件夹不能被压缩: {}", dir);
            return "当前日期的文件夹不能被压缩";
        }
        
        String filePath = String.join("/", this.fileDir, dir);
        File file = null;
        
        try {
            file = FileUtility.openFile(filePath);
            
            // 文件存在性检查
            if (!file.exists()) {
                log.info("要压缩的文件夹不存在: {}", dir);
                return "文件夹不存在";
            }
            
            // 隐藏文件检查
            if (file.isHidden()){
                log.info("要压缩的文件夹是隐藏的: {}", dir);
                return "文件夹是隐藏的";
            }
            
            // 检查文件夹是否为空
            File[] files = file.listFiles();
            if (files == null || files.length == 0) {
                log.info("文件夹为空，无需压缩: {}", dir);
                return "文件夹为空，无需压缩";
            }
            
            // 生成压缩文件路径
            String zipFilePath = String.format("%s/%s.zip", this.fileDir, file.getName());
            File zipFile = new File(zipFilePath);
            
            // 检查目标zip文件是否已存在
            if (zipFile.exists()) {
                log.info("压缩文件已存在，将被覆盖: {}", zipFilePath);
            }
            
            // 执行压缩
            FileUtility.zipFolder(filePath, zipFilePath);
            
            log.info("文件夹压缩成功: {} -> {}", filePath, zipFilePath);
            return "压缩成功";
        } catch (Exception e) {
            log.error("压缩文件夹失败: {}", dir, e);
            return "压缩文件夹失败: " + e.getMessage();
        }
    }
    
    /**
     * 手动触发缓存刷新
     * @return 刷新结果
     */
    public String flushCache() {
        try {
            int cacheCount = cacheSize.get();
            saveCaches();
            return String.format("成功刷新%d个缓存对象到磁盘", cacheCount);
        } catch (Exception e) {
            log.error("手动刷新缓存失败", e);
            return "刷新缓存失败: " + e.getMessage();
        }
    }
}
