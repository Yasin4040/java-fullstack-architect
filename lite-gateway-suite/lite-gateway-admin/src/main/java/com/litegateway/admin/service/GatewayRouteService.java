package com.litegateway.admin.service;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.litegateway.admin.common.web.PageBody;
import com.litegateway.admin.dto.InstanceDTO;
import com.litegateway.admin.dto.InterfaceDTO;
import com.litegateway.admin.dto.RouteDTO;
import com.litegateway.admin.query.InstanceQuery;
import com.litegateway.admin.query.RouteQuery;
import com.litegateway.admin.vo.RouteVO;

import java.util.List;

/**
 * 网关路由服务接口
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway
 * 移除了公司特定的注解和依赖
 */
public interface GatewayRouteService {

    /**
     * 添加路由规则
     * @param routeDTO 路由DTO
     */
    void addRoute(RouteDTO routeDTO);

    /**
     * 获取路由列表
     * @return 路由列表
     */
    List<RouteDTO> routeList();

    /**
     * 删除路由
     * @param id 路由ID
     */
    void deleteRoute(Long id);

    /**
     * 重新加载配置
     */
    void reloadConfig();

    /**
     * 分页查询路由
     * @param query 查询参数
     * @return 分页结果
     */
    PageBody<RouteVO> selectRoutePageVo(RouteQuery query);

    /**
     * 解析接口信息
     * @param result Swagger API Docs JSON
     * @param path 路径过滤
     * @param summary 摘要过滤
     * @param prePath 前缀路径
     * @return 接口列表
     */
    List<InterfaceDTO> mapToInterfaceDTO(String result, String path, String summary, String prePath);

    /**
     * 更新路由
     * @param routeDTO 路由DTO
     */
    void updateRoute(RouteDTO routeDTO);

    /**
     * 获取所有实例
     * @param serviceName 服务名
     * @return 实例列表
     */
    List<Instance> getAllInstances(String serviceName);

    /**
     * 分页获取实例
     * @param query 查询参数
     * @return 实例列表
     */
    List<Instance> getAllInstancesPage(InstanceQuery query);

    /**
     * 更新实例权重
     * @param dto 实例DTO
     */
    void updateInstanceWeight(InstanceDTO dto);

    /**
     * 更新实例启用状态
     * @param dto 实例DTO
     */
    void updateInstanceEnabled(InstanceDTO dto);

    /**
     * 根据ID获取路由
     * @param id 路由ID
     * @return 路由DTO
     */
    RouteDTO getById(String id);


    List<RouteVO> selectRouteList(RouteQuery query);
}
