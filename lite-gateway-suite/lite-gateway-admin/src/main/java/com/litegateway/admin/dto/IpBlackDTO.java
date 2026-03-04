package com.litegateway.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * IP黑名单 DTO
 */
@Data
@Schema(description = "IP黑名单数据传输对象")
public class IpBlackDTO {

    @Schema(description = "IP地址")
    private String ip;

    @Schema(description = "备注")
    private String remark;
}
