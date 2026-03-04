package com.litegateway.admin.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 实例查询参数
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "实例查询参数")
public class InstanceQuery extends PageQuery {

    @Schema(description = "服务名称")
    private String serviceName;

    @Schema(description = "分组名称", example = "local")
    private String groupName = "local";

    @Schema(description = "集群名称", example = "DEFAULT")
    private String clusterName = "DEFAULT";
}
