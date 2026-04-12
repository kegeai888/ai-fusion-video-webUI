package com.stonewu.fusion.controller.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "更新AI模型请求")
@Data
public class AiModelUpdateReqVO {
    @NotNull(message = "模型ID不能为空")
    private Long id;
    private String name;
    private String code;
    private Integer modelType;
    private String icon;
    private String description;
    private Integer sort;
    private Integer status;
    private String config;
    private Boolean defaultModel;
    private Long apiConfigId;
}
