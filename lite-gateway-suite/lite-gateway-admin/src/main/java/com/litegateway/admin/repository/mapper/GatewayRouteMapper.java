package com.litegateway.admin.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.litegateway.admin.repository.entity.GatewayRoute;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 网关路由 Mapper
 */
@Mapper
public interface GatewayRouteMapper extends BaseMapper<GatewayRoute> {

    /**
     * 查询启用的路由列表
     */
    @Select("SELECT * FROM gateway_route WHERE status = 0 AND deleted = 0 ORDER BY create_time DESC")
    List<GatewayRoute> selectEnabledRoutes();
}
