package com.litegateway.test.userservice.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${server.port}")
    private String port;

    // 模拟用户数据存储
    private static final Map<Long, User> userStore = new HashMap<>();

    static {
        userStore.put(1L, new User(1L, "张三", "zhangsan@example.com", "13800138001"));
        userStore.put(2L, new User(2L, "李四", "lisi@example.com", "13800138002"));
        userStore.put(3L, new User(3L, "王五", "wangwu@example.com", "13800138003"));
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("service", serviceName);
        result.put("port", port);
        result.put("status", "UP");
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }

    @GetMapping
    public Map<String, Object> listUsers() {
        log.info("获取用户列表");
        Map<String, Object> result = new HashMap<>();
        result.put("service", serviceName);
        result.put("port", port);
        result.put("data", new ArrayList<>(userStore.values()));
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getUser(@PathVariable Long id) {
        log.info("获取用户详情: {}", id);
        Map<String, Object> result = new HashMap<>();
        result.put("service", serviceName);
        result.put("port", port);
        result.put("data", userStore.get(id));
        return result;
    }

    @PostMapping
    public Map<String, Object> createUser(@RequestBody User user) {
        log.info("创建用户: {}", user);
        Long id = (long) (userStore.size() + 1);
        user.setId(id);
        userStore.put(id, user);

        Map<String, Object> result = new HashMap<>();
        result.put("service", serviceName);
        result.put("port", port);
        result.put("message", "用户创建成功");
        result.put("data", user);
        return result;
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateUser(@PathVariable Long id, @RequestBody User user) {
        log.info("更新用户: {}", id);
        user.setId(id);
        userStore.put(id, user);

        Map<String, Object> result = new HashMap<>();
        result.put("service", serviceName);
        result.put("port", port);
        result.put("message", "用户更新成功");
        result.put("data", user);
        return result;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteUser(@PathVariable Long id) {
        log.info("删除用户: {}", id);
        userStore.remove(id);

        Map<String, Object> result = new HashMap<>();
        result.put("service", serviceName);
        result.put("port", port);
        result.put("message", "用户删除成功");
        return result;
    }

    @GetMapping("/profile")
    public Map<String, Object> getProfile() {
        Map<String, Object> result = new HashMap<>();
        result.put("service", serviceName);
        result.put("port", port);
        result.put("user", userStore.get(1L));
        result.put("permissions", Arrays.asList("user:read", "user:write"));
        return result;
    }

    @Data
    public static class User {
        private Long id;
        private String name;
        private String email;
        private String phone;

        public User() {}

        public User(Long id, String name, String email, String phone) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phone = phone;
        }
    }
}
