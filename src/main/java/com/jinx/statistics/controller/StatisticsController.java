package com.jinx.statistics.controller;

import com.jinx.statistics.response.ApiResponse;
import com.jinx.statistics.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * 统计数据（支持Map格式）
     * @param data 统计数据（键值对）
     * @return 返回成功
     */
    @PostMapping(value = "/log", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "统计数据（Map格式）", 
        description = "记录统计数据，接收键值对格式的数据"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "统计成功",
            content = @Content(schema = @Schema(implementation = com.jinx.statistics.response.ApiResponse.class))
        )
    })
    public ApiResponse log(@RequestBody Map<String, String> data) {
        statisticsService.log(data);
        return ApiResponse.success();
    }

    /**
     * 获取文件夹内文件列表
     * @param path 文件夹路径
     * @return 用于网页显示的文本内容
     */
    @GetMapping(value = "/list", produces = "text/html;charset=UTF-8")
    @Operation(
        summary = "获取文件夹内文件列表", 
        description = "根据提供的路径获取文件夹内的文件列表，返回HTML格式的内容用于网页显示"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "成功获取文件列表",
            content = @Content(mediaType = "text/html;charset=UTF-8")
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "文件夹不存在"
        )
    })
    public String list(@Parameter(description = "文件夹路径", required = true) String path){
        return statisticsService.list(path);
    }

    /**
     * 下载文件
     * @param path 文件夹名
     * @return 文件
     */
    @GetMapping("/download")
    @Operation(
        summary = "下载文件", 
        description = "根据提供的路径下载指定文件"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "文件下载成功",
            content = @Content(mediaType = "application/octet-stream")
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "文件不存在"
        )
    })
    public ResponseEntity<Resource> download(@Parameter(description = "文件路径", required = true) String path){
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
    @Operation(
        summary = "压缩文件夹", 
        description = "将指定路径的文件夹压缩成zip文件"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "压缩成功",
            content = @Content(schema = @Schema(implementation = com.jinx.statistics.response.ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "文件夹不存在或压缩失败"
        )
    })
    public com.jinx.statistics.response.ApiResponse<String> zip(@Parameter(description = "文件夹路径", required = true) @PathVariable String dir){
        String result = statisticsService.zip(dir);
        return ApiResponse.success(result);
    }
}
