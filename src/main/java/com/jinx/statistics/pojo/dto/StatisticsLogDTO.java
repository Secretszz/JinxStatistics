package com.jinx.statistics.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "数据统计时传递的数据模型")
public class StatisticsLogDTO {

    @Schema(description = "日志表名")
    private String name;

    @Schema(description = "日志内容")
    private String value;
}
