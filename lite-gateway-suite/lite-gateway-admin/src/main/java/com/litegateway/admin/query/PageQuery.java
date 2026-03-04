package com.litegateway.admin.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分页查询基类
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway
 */
@Data
@Schema(description = "分页查询参数")
public class PageQuery {

    @Schema(description = "当前页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "20")
    private Integer pageSize = 20;
}
