package com.litegateway.admin.controller;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.litegateway.admin.common.web.PageBody;
import com.litegateway.admin.common.web.Result;
import com.litegateway.admin.dto.InstanceDTO;
import com.litegateway.admin.dto.InterfaceDTO;
import com.litegateway.admin.dto.RouteDTO;
import com.litegateway.admin.query.InstanceQuery;
import com.litegateway.admin.query.RouteQuery;
import com.litegateway.admin.service.GatewayRouteService;
import com.litegateway.admin.vo.RouteVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * 网关路由管理控制器
 *
 */
@Slf4j
@RestController
@RequestMapping("/gateway/route")
@Tag(name = "路由管理", description = "网关路由管理相关接口")
public class GatewayRouteController {

    @Autowired
    private GatewayRouteService gatewayRouteService;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 分页查询路由
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询路由", description = "分页查询网关路由列表")
    public Result<PageBody<RouteVO>> selectRoutePageVo(@Validated @ModelAttribute RouteQuery query) {
        PageBody<RouteVO> routePage = gatewayRouteService.selectRoutePageVo(query);
        return Result.ok(routePage);
    }

    /**
     * 分页查询路由
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询路由", description = "分页查询网关路由列表")
    public Result<List<RouteVO>> selectRouteList(@Validated @ModelAttribute RouteQuery query) {
        List<RouteVO> routeList = gatewayRouteService.selectRouteList(query);
        return Result.ok(routeList);
    }

    /**
     * 获取单个路由
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取单个路由", description = "根据ID获取路由详情")
    public Result<RouteDTO> getById(@PathVariable String id) {
        RouteDTO routeDTO = gatewayRouteService.getById(id);
        return Result.ok(routeDTO);
    }

    /**
     * 添加路由
     */
    @PostMapping
    @Operation(summary = "添加路由", description = "添加新的网关路由")
    public Result<Void> addRoute(@Validated @RequestBody RouteDTO routeDTO) {
        gatewayRouteService.addRoute(routeDTO);
        gatewayRouteService.reloadConfig();
        return Result.ok();
    }

    /**
     * 修改路由
     */
    @PutMapping("/{id}")
    @Operation(summary = "修改路由", description = "修改网关路由")
    public Result<Void> updateRoute(@PathVariable Long id, @Validated @RequestBody RouteDTO routeDTO) {
        routeDTO.setId(id);
        gatewayRouteService.updateRoute(routeDTO);
        gatewayRouteService.reloadConfig();
        return Result.ok();
    }

    /**
     * 修改路由状态
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "修改路由状态", description = "启用或禁用路由")
    public Result<Void> updateRouteStatus(@PathVariable Long id, @RequestParam String status) {
        RouteDTO route = new RouteDTO();
        route.setId(id);
        route.setStatus(status);
        gatewayRouteService.updateRoute(route);
        gatewayRouteService.reloadConfig();
        return Result.ok();
    }

    /**
     * 删除路由
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除路由", description = "删除网关路由")
    public Result<Void> deleteRoute(@PathVariable Long id) {
        gatewayRouteService.deleteRoute(id);
        gatewayRouteService.reloadConfig();
        return Result.ok();
    }

    /**
     * 刷新配置
     */
    @PostMapping("/reload")
    @Operation(summary = "刷新配置", description = "重新加载网关路由配置")
    public Result<Void> reloadConfig(Principal principal) {
        // 使用 Principal 获取当前登录用户（基于 JWT）
        String username = principal != null ? principal.getName() : "system";
        log.info("User {} triggered route config reload", username);
        gatewayRouteService.reloadConfig();
        return Result.ok();
    }

    /**
     * 获取服务所有接口
     */
    @GetMapping("/{id}/interfaces")
    @Operation(summary = "获取服务所有接口", description = "从 Swagger API Docs 获取服务接口列表")
    public Result<PageBody<InterfaceDTO>> getAllInterface(
            @PathVariable String id,
            @RequestParam(name = "path", required = false, defaultValue = "") String path,
            @RequestParam(name = "summary", required = false, defaultValue = "") String summary) {

        RouteDTO gatewayRoute = gatewayRouteService.getById(id);
        if (gatewayRoute == null) {
            return Result.failure("G0001", "路由不存在");
        }

        String serviceUrl = gatewayRoute.getUri();
        serviceUrl = serviceUrl.replaceFirst("lb://", "http://");
        String prePath = gatewayRoute.getPath();

        try {
            String restResult = restTemplate.getForObject(serviceUrl + "/v3/api-docs", String.class);
            List<InterfaceDTO> interfaces = gatewayRouteService.mapToInterfaceDTO(restResult, path, summary, prePath);
            return Result.ok(new PageBody<>(interfaces));
        } catch (Exception e) {
            log.error("Failed to get interfaces: {}", e.getMessage());
            return Result.failure("G0004", "获取接口信息失败");
        }
    }

    /**
     * 获取服务所有实例
     */
    @GetMapping("/instances")
    @Operation(summary = "获取服务所有实例", description = "从 Nacos 获取服务实例列表")
    public Result<PageBody<Instance>> getAllInstances(@RequestParam String serviceName) {
        List<Instance> instances = gatewayRouteService.getAllInstances(serviceName);
        return Result.ok(new PageBody<>(instances));
    }

    /**
     * 分页获取服务实例
     */
    @GetMapping("/instances/page")
    @Operation(summary = "分页获取服务实例", description = "分页查询 Nacos 服务实例")
    public Result<PageBody<Instance>> getAllInstancesPage(@ModelAttribute InstanceQuery query) {
        List<Instance> records = new ArrayList<>();
        try {
            records = gatewayRouteService.getAllInstancesPage(query);
        } catch (Exception e) {
            log.error("Failed to get instances page: {}", e.getMessage());
        }

        int total = records.size();
        int pages = query.getPageSize() > 0 ? (total + query.getPageSize() - 1) / query.getPageSize() : 1;

        return Result.ok(new PageBody<>((long) total, pages, query.getPageSize(), query.getPageNum(), records));
    }

    /**
     * 更新实例权重
     */
    @PostMapping("/instances/weight")
    @Operation(summary = "更新实例权重", description = "修改 Nacos 服务实例权重")
    public Result<Void> updateInstanceWeight(@RequestBody InstanceDTO dto) {
        gatewayRouteService.updateInstanceWeight(dto);
        return Result.ok();
    }

    /**
     * 更新实例启用状态
     */
    @PostMapping("/instances/enabled")
    @Operation(summary = "实例上下线", description = "启用或禁用 Nacos 服务实例")
    public Result<Void> updateInstanceEnabled(@RequestBody InstanceDTO dto) {
        gatewayRouteService.updateInstanceEnabled(dto);
        return Result.ok();
    }
}
