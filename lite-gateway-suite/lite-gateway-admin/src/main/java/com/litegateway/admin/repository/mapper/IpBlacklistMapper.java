package com.litegateway.admin.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.litegateway.admin.repository.entity.IpBlacklist;
import org.apache.ibatis.annotations.Mapper;

/**
 * IP黑名单 Mapper
 */
@Mapper
public interface IpBlacklistMapper extends BaseMapper<IpBlacklist> {
}
