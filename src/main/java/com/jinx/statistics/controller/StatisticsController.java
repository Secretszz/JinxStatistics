package com.jinx.statistics.controller;

import com.jinx.statistics.exception.BaseException;
import com.jinx.statistics.pojo.dto.StatisticsLogDTO;
import com.jinx.statistics.response.ApiResponse;
import com.jinx.statistics.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * 统计数据
     * @param logDTO 统计数据
     * @return
     */
    @PostMapping("/log")
    @Operation(summary = "统计数据")
    public ApiResponse<String> log(StatisticsLogDTO logDTO){
        statisticsService.log(logDTO);
        return ApiResponse.success();
    }

    /**
     * 获取文件夹内文件列表
     * @return 用于网页显示的文本内容
     */
    @GetMapping(value = "/list", produces = "text/html;charset=UTF-8")
    @Operation(summary = "获取文件夹列表")
    public String list(){
        return statisticsService.list("");
    }

    /**
     * 获取文件夹内文件列表
     * @param dir 文件夹名
     * @return 用于网页显示的文本内容
     */
    @GetMapping(value = "/list/{dir}", produces = "text/html;charset=UTF-8")
    @Operation(summary = "获取文件夹内文件列表")
    public String list(@PathVariable(required = false) String dir){
        return statisticsService.list(dir);
    }

    /**
     * 下载文件
     * @param dir 文件夹名
     * @param file 文件名
     * @return 文件
     */
    @GetMapping("/download/{dir}/{file}")
    @Operation(summary = "下载文件")
    public ResponseEntity<Resource> download(@PathVariable String dir, @PathVariable String file){
        Resource resource = statisticsService.download(dir, file);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file);
        try {
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(resource.contentLength())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (IOException e) {
            throw new BaseException(e);
        }
    }

    /**
     * 压缩文件夹
     * @param dir
     * @return
     */
    @PutMapping("/zip/{dir}")
    @Operation(summary = "压缩文件夹")
    public ApiResponse<String> zip(@PathVariable String dir){
        String result = statisticsService.zip(dir);
        return ApiResponse.success(result);
    }
}
