package com.litegateway.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 白名单 DTO
 */
@Data
@Schema(description = "白名单数据传输对象")
public class WhiteListDTO {

    @Schema(description = "路径")
    private String path;

    @Schema(description = "描述")
    private String description;
}
