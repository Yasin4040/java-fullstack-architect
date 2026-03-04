package com.litegateway.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 实例数据传输对象
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway
 */
@Data
@Schema(description = "实例数据传输对象")
public class InstanceDTO {

    @Schema(description = "实例ID")
    private String instanceId;

    @Schema(description = "服务名称")
    private String serviceName;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "权重")
    private Double weight;
}
