package com.stonewu.fusion.controller.storyboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "更新分镜集请求")
@Data
public class StoryboardEpisodeUpdateReqVO {

    private Long id;

    private Integer episodeNumber;

    private String title;

    private String synopsis;

    private Integer sortOrder;
}
