package com.jinx.statistics.controller;

import com.jinx.statistics.pojo.dto.StatisticsLogDTO;
import com.jinx.statistics.response.ApiResponse;
import com.jinx.statistics.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * 统计数据
     * @param list 统计数据
     * @return 返回成功
     */
    @PostMapping("/log")
    @Operation(summary = "统计数据")
    public ApiResponse<String> log(List<StatisticsLogDTO> list){
        statisticsService.log(list);
        return ApiResponse.success();
    }

    /**
     * 获取文件夹内文件列表
     * @param path 文件夹路径
     * @return 用于网页显示的文本内容
     */
    @GetMapping(value = "/list", produces = "text/html;charset=UTF-8")
    @Operation(summary = "获取文件夹内文件列表")
    public String list(String path){
        return statisticsService.list(path);
    }

    /**
     * 下载文件
     * @param path 文件夹名
     * @return 文件
     */
    @GetMapping("/download")
    @Operation(summary = "下载文件")
    public ResponseEntity<Resource> download(String path){
        File file = statisticsService.download(path);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(file));
    }

    /**
     * 压缩文件夹
     * @param dir 文件夹路径
     * @return 压缩结果
     */
    @PutMapping("/zip/{dir}")
    @Operation(summary = "压缩文件夹")
    public ApiResponse<String> zip(@PathVariable String dir){
        String result = statisticsService.zip(dir);
        return ApiResponse.success(result);
    }
}
