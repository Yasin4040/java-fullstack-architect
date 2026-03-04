package com.litegateway.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 接口信息传输对象
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway
 */
@Data
@Schema(description = "接口信息传输对象")
public class InterfaceDTO {

    @Schema(description = "请求路径")
    private String path;

    @Schema(description = "请求名称")
    private String summary;

    @Schema(description = "http类型")
    private String type;

    @Schema(description = "标志分组")
    private String tag;

    @Schema(description = "是否添加 0 YES 1 No")
    private String ifAdd;
}
