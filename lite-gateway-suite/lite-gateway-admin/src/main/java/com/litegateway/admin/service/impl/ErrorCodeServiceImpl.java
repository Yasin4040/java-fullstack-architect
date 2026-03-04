package com.litegateway.admin.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.litegateway.admin.common.exception.ErrorCodeDefinition;
import com.litegateway.admin.common.web.Result;
import com.litegateway.admin.service.ErrorCodeService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;


@Slf4j
@Service
public class ErrorCodeServiceImpl implements ErrorCodeService {

    private volatile Map<String, ErrorCodeDefinition> errorCodeMap = new HashMap<>();

    private static final String YAML_CONFIG_PATH = "error-codes.yml";

    private static final String DEFAULT_SUCCESS_CODE = "00000";

    /**
     * 初始化加载错误码配置
     */
    @PostConstruct
    public void init() {
        refresh();
    }

    @Override
    public void refresh() {
        Map<String, ErrorCodeDefinition> newMap = new HashMap<>();

        // 1. 先加载内置默认错误码（作为兜底）
        loadDefaultErrorCodes(newMap);

        // 2. 加载 error-codes.yml 文件（覆盖默认）
        loadFromYamlFile(newMap);

        this.errorCodeMap = Collections.unmodifiableMap(newMap);

        // 3. 设置成功码到 Result 类
        setupSuccessCode(newMap);

        log.info("Loaded {} error codes", newMap.size());
    }

    /**
     * 设置成功码
     */
    private void setupSuccessCode(Map<String, ErrorCodeDefinition> map) {
        // 查找标记为 success=true 的配置
        for (ErrorCodeDefinition definition : map.values()) {
            if (Boolean.TRUE.equals(definition.getSuccess())) {
                Result.setSuccessCode(definition.getCode(), definition.getMessage());
                log.info("Success code configured: {} - {}", definition.getCode(), definition.getMessage());
                return;
            }
        }

        // 使用默认成功码
        Result.setSuccessCode(DEFAULT_SUCCESS_CODE, "成功");
        log.info("Using default success code: {}", DEFAULT_SUCCESS_CODE);
    }

    /**
     * 从 error-codes.yml 文件加载配置
     * 文件不存在或读取失败时忽略
     */
    private void loadFromYamlFile(Map<String, ErrorCodeDefinition> map) {
        try {
            Resource resource = new ClassPathResource(YAML_CONFIG_PATH);
            if (!resource.exists()) {
                log.debug("YAML config file not found: {}", YAML_CONFIG_PATH);
                return;
            }

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            List<ErrorCodeDefinition> codes;

            try (InputStream is = resource.getInputStream()) {
                codes = mapper.readValue(is, new TypeReference<List<ErrorCodeDefinition>>() {});
            }

            if (codes == null || codes.isEmpty()) {
                log.debug("YAML config file is empty");
                return;
            }

            for (ErrorCodeDefinition code : codes) {
                if (code.getCode() != null) {
                    map.put(code.getCode(), code);
                }
            }

            log.info("Loaded {} error codes from YAML file: {}", codes.size(), YAML_CONFIG_PATH);
        } catch (IOException e) {
            log.warn("Failed to load error codes from YAML file: {}", e.getMessage());
        }
    }

    /**
     * 加载默认错误码
     */
    private void loadDefaultErrorCodes(Map<String, ErrorCodeDefinition> map) {
        // 成功码
        addDefaultCode(map, "00000", "成功", "info", "none", null, true);

        // 认证相关
        addDefaultCode(map, "A0201", "用户账户不存在", "warning");
        addDefaultCode(map, "A0202", "用户账户被冻结", "warning");
        addDefaultCode(map, "A0210", "用户密码错误", "warning");
        addDefaultCode(map, "A0220", "用户身份校验失败", "warning");
        addDefaultCode(map, "A0230", "用户登录已过期", "warning", "logout");
        addDefaultCode(map, "A0231", "用户未登录", "warning", "logout");
        addDefaultCode(map, "A0240", "用户验证码错误", "warning");

        // 权限相关
        addDefaultCode(map, "A0300", "访问权限异常", "warning");
        addDefaultCode(map, "A0301", "访问未授权", "warning", "redirect", "/403");

        // 参数相关
        addDefaultCode(map, "A0400", "用户请求参数错误", "warning");
        addDefaultCode(map, "A0404", "找不到路径", "warning");
        addDefaultCode(map, "A0410", "请求必填参数为空", "warning");
        addDefaultCode(map, "A0421", "参数格式不匹配", "warning");

        // 系统错误
        addDefaultCode(map, "B0001", "系统执行出错", "error");
        addDefaultCode(map, "B0100", "系统执行超时", "warning");
        addDefaultCode(map, "B0210", "系统限流", "warning");

        // 服务错误
        addDefaultCode(map, "C0001", "调用第三方服务出错", "error");
        addDefaultCode(map, "C0300", "数据库服务出错", "error");

        // 网关错误
        addDefaultCode(map, "G0001", "网关路由不存在", "warning");
        addDefaultCode(map, "G0002", "网关路由已存在", "warning");
        addDefaultCode(map, "G0003", "Nacos服务查询失败", "error");
        addDefaultCode(map, "G0004", "路由配置解析失败", "error");
    }

    private void addDefaultCode(Map<String, ErrorCodeDefinition> map, String code,
                                 String message, String level) {
        addDefaultCode(map, code, message, level, "none", null, false);
    }

    private void addDefaultCode(Map<String, ErrorCodeDefinition> map, String code,
                                 String message, String level, String action) {
        addDefaultCode(map, code, message, level, action, null, false);
    }

    private void addDefaultCode(Map<String, ErrorCodeDefinition> map, String code,
                                 String message, String level, String action, String redirectUrl) {
        addDefaultCode(map, code, message, level, action, redirectUrl, false);
    }

    private void addDefaultCode(Map<String, ErrorCodeDefinition> map, String code,
                                 String message, String level, String action,
                                 String redirectUrl, boolean success) {
        ErrorCodeDefinition definition = new ErrorCodeDefinition();
        definition.setCode(code);
        definition.setMessage(message);
        definition.setLevel(level);
        definition.setAction(action);
        definition.setRedirectUrl(redirectUrl);
        definition.setSuccess(success);
        definition.setShowNotification(!success);
        definition.setDuration(3);
        definition.setLogStackTrace("error".equals(level) || "fatal".equals(level));

        map.put(code, definition);
    }

    @Override
    public List<ErrorCodeDefinition> getAllErrorCodes() {
        return new ArrayList<>(errorCodeMap.values());
    }

    @Override
    public Map<String, ErrorCodeDefinition> getErrorCodeMap() {
        return errorCodeMap;
    }

    @Override
    public ErrorCodeDefinition getErrorCode(String code) {
        return errorCodeMap.get(code);
    }

    @Override
    public ErrorCodeDefinition getSuccessCode() {
        for (ErrorCodeDefinition definition : errorCodeMap.values()) {
            if (Boolean.TRUE.equals(definition.getSuccess())) {
                return definition;
            }
        }
        return errorCodeMap.get(DEFAULT_SUCCESS_CODE);
    }
}
