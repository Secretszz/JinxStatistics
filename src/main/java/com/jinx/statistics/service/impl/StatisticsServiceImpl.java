package com.jinx.statistics.service.impl;

import com.jinx.statistics.constant.MessageConstant;
import com.jinx.statistics.dao.StatisticsDao;
import com.jinx.statistics.exception.BaseException;
import com.jinx.statistics.service.StatisticsService;
import com.jinx.statistics.utility.StringUtility;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Service
@EnableScheduling
public class StatisticsServiceImpl implements StatisticsService {

    private final StatisticsDao dao;

    /**
     * 文档对象的读写锁，保证线程安全
     */
    private final ReadWriteLock documentLock = new ReentrantReadWriteLock();
    
    /**
     * HTML文档对象
     * volatile保证可见性
     */
    private volatile Document document;

    /**
     * 日期格式化器的ThreadLocal，避免SimpleDateFormat的线程安全问题
     */
    private final ThreadLocal<SimpleDateFormat> dateFormatter = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyyMMdd"));

    public StatisticsServiceImpl(StatisticsDao dao) {
        this.dao = dao;
        // 初始化HTML模板
        initHtmlTemplate();
    }
    
    /**
     * 初始化HTML模板
     */
    private void initHtmlTemplate() {
        String template = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>下载数据统计</title>
                </head>
                <body>

                </body>
                </html>""";

        documentLock.writeLock().lock();
        try {
            document = Jsoup.parse(template);
            log.info("HTML模板初始化成功");
        } finally {
            documentLock.writeLock().unlock();
        }
    }

    /**
     * 统计日志
     * 线程安全的实现，处理并发请求
     *
     * @param data 数据
     */
    @Override
    public void log(Map<String, String> data) {
        // 参数校验
        if (data == null || data.isEmpty()) {
            log.warn("统计日志参数无效: data={}", data != null ? 0 : "null");
            return;
        }
        
        try {
            // 使用线程安全的方式处理每个统计项
            for (Map.Entry<String, String> entry : data.entrySet()) {
                String name = entry.getKey();
                String value = entry.getValue();

                if (!StringUtils.hasLength(name) || !StringUtils.hasLength(value)) {
                    continue;
                }

                // 使用dao的线程安全方法添加统计数据
                dao.appendStatistics(name, value);
            }
        } catch (Exception e) {
            log.error("处理统计日志时发生错误", e);
        }
    }

    /**
     * 获取文件夹内文件列表
     * 线程安全的实现，处理并发请求
     *
     * @param dirName 文件夹名
     * @return 用于网页显示的文本内容
     */
    @Override
    public String list(String dirName) {
        // 参数规范化
        final String normalizedDirName = dirName == null ? "" : dirName;
        
        try {
            final String refDownload = "/statistics/download?path=";
            final String refList = "/statistics/list?path=";
            final String currentDate = nowStrYMD();

            // 获取文件列表
            File[] files = dao.getFiles(normalizedDirName);
            if (files == null) {
                throw new BaseException("无法获取文件列表");
            }
            
            final int length = files.length;
            final Object[] empty = new Object[0];
            
            // 使用线程安全的StringBuilder代替StringBuffer
            StringBuilder sb = new StringBuilder(1024); // 预分配合理的初始容量
            StringUtility.appendLine(sb, "<h1>文件下载</h1>", empty);
            StringUtility.appendLine(sb, "<div>文件列表数量: %s</div>", new Object[]{length});
            StringUtility.appendLine(sb, "<ul>", empty);
            
            // 添加返回上级目录链接
            if (StringUtils.hasLength(normalizedDirName)) {
                StringUtility.appendLine(sb, "<li><a href=\"%s\">..</a></li>", new Object[]{refList});
                
                // 非当前日期的文件夹可以下载压缩包
                if (!normalizedDirName.equals(currentDate)){
                    StringUtility.appendLine(sb, "<li><a href=\"%s\">%s</a></li>", 
                            new Object[]{refDownload.concat(normalizedDirName + ".zip"), "下载zip全部文件"});
                }
            }
            
            // 添加文件列表
            for (File file : files) {
                final boolean isDir = file.isDirectory();
                final String name = file.getName();
                final String ref;
                
                if (isDir){
                    ref = refList.concat(name);
                } else {
                    if (StringUtils.hasLength(normalizedDirName)) {
                        ref = refDownload.concat(String.join("/", normalizedDirName, name));
                    } else {
                        ref = refDownload.concat(name);
                    }
                }
                
                StringUtility.appendLine(sb, "<li><a href=\"%s\">%s</a></li>", new Object[]{ref, name});
            }
            
            StringUtility.appendLine(sb, "</ul>", empty);
            
            // 使用线程安全的方式生成HTML
            return listHtml(sb.toString());
        } catch (Exception e) {
            log.error("获取文件列表失败: {}", normalizedDirName, e);
            throw new BaseException(e);
        }
    }

    /**
     * 下载文件
     * 线程安全的实现
     *
     * @param path  文件路径
     * @return 文件
     */
    @Override
    public File download(String path) {
        if (path == null || path.isEmpty()) {
            log.warn("下载文件路径为空");
            throw new BaseException(MessageConstant.EMPTY_FILE);
        }
        
        try {
            // 获取文件是线程安全的操作
            File file = dao.getFile(path);
            
            // 检查文件是否存在
            if (!file.exists()) {
                log.warn("请求下载的文件不存在: {}", path);
                throw new BaseException(MessageConstant.EMPTY_FILE);
            }
            
            // 检查文件是否可读
            if (!file.canRead()) {
                log.warn("请求下载的文件不可读: {}", path);
                throw new BaseException("文件不可读");
            }
            
            return file;
        } catch (Exception e) {
            log.error("下载文件失败: {}", path, e);
            throw new BaseException(e);
        }
    }

    /**
     * 压缩src/main/resources/backups/statistics/文件夹下的文件夹
     * 线程安全的实现
     *
     * @param dir 文件夹名
     * @return 压缩结果
     */
    @Override
    public String zip(String dir) {
        if (dir == null || dir.isEmpty()) {
            return "文件夹名称不能为空";
        }
        
        try {
            // dao.zipFile方法已经是线程安全的
            return dao.zipFile(dir);
        } catch (Exception e) {
            log.error("压缩文件夹失败: {}", dir, e);
            return "压缩失败: " + e.getMessage();
        }
    }

    /**
     * 获取当前日期字符串，格式为yyyyMMdd
     * 使用ThreadLocal确保线程安全
     * 
     * @return 日期字符串
     */
    private String nowStrYMD(){
        return dateFormatter.get().format(new Date());
    }

    /**
     * 将内容包装到HTML模板中
     * 使用读写锁确保线程安全
     * 
     * @param bodyContent HTML内容
     * @return 完整的HTML页面
     */
    private String listHtml(String bodyContent) {
        documentLock.readLock().lock();
        try {
            if (document == null) {
                log.warn("HTML模板未初始化，尝试重新初始化");
                documentLock.readLock().unlock();
                initHtmlTemplate();
                documentLock.readLock().lock();
                
                if (document == null) {
                    log.error("HTML模板初始化失败");
                    return "<html><body>" + bodyContent + "</body></html>";
                }
            }
            
            // 创建文档副本，避免并发修改
            Document docCopy = document.clone();
            Element body = docCopy.body();
            body.html(""); // 清空body
            body.append(bodyContent);
            return docCopy.outerHtml();
        } finally {
            documentLock.readLock().unlock();
        }
    }
}
