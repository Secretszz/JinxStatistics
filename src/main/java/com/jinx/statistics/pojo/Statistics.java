package com.jinx.statistics.pojo;

import com.jinx.statistics.utility.FileUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.Serializable;

@Slf4j
public class Statistics implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String date;
    private final String name;
    private final String filePath;
    private File file;
    private final StringBuffer _builder = new StringBuffer();

    public Statistics(String date, String name, String value, String fileDir) {
        this.date = date;
        this.name = name;
        this.filePath = String.join("/", fileDir, date, name + ".csv");
        try {
            this.file = FileUtility.getFile(this.filePath);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        this._builder.append(value).append("\r\n");
    }

    /**
     * 增加记录
     * @param value 记录值
     */
    public void append(String value) {
        this._builder.append(value).append("\r\n");
    }

    /**
     * 保存文件
     */
    public void saveFile(){
        if(this.file == null){
            log.info(String.format("=== [%s] file is null", this.filePath));
        } else {
            String _v = this._builder.toString();
            if (StringUtils.hasLength(_v)) {
                this._builder.delete(0, _v.length());

                try {
                    FileUtility.writeFully(this.file, _v, true);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
                log.info(String.format("=== [%s] file saved", this.filePath));
            }
        }
    }
}
