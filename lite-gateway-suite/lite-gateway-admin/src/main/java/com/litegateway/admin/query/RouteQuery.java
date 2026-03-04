package com.litegateway.admin.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 路由查询参数
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "路由查询参数")
public class RouteQuery extends PageQuery {

    @Schema(description = "状态，0启用，1禁用")
    private String status;

    @Schema(description = "服务名称")
    private String name;

    @Schema(description = "服务地址")
    private String uri;

    @Schema(description = "断言path")
    private String path;
}
