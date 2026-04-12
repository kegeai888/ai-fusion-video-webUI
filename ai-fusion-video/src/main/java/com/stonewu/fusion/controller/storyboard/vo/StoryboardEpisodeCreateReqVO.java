package com.stonewu.fusion.controller.storyboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "创建分镜集请求")
@Data
public class StoryboardEpisodeCreateReqVO {

    private Long storyboardId;

    private Integer episodeNumber;

    private String title;

    private String synopsis;

    private Integer sortOrder;
}
