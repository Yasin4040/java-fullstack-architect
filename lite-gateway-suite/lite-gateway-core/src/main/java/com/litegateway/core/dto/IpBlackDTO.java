package com.litegateway.core.dto;

import lombok.Data;

/**
 * IP黑名单 DTO
 */
@Data
public class IpBlackDTO {

    private String ip;

    private String remark;
}
