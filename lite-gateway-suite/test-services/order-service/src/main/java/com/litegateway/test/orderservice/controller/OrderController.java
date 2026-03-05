package com.litegateway.test.orderservice.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${server.port}")
    private String port;

    // 模拟订单数据存储
    private static final Map<Long, Order> orderStore = new HashMap<>();

    static {
        orderStore.put(1L, new Order(1L, "ORD-2024-001", 1L, new BigDecimal("199.99"), "PAID", LocalDateTime.now().minusDays(1)));
        orderStore.put(2L, new Order(2L, "ORD-2024-002", 2L, new BigDecimal("299.99"), "PENDING", LocalDateTime.now().minusHours(5)));
        orderStore.put(3L, new Order(3L, "ORD-2024-003", 1L, new BigDecimal("599.99"), "SHIPPED", LocalDateTime.now().minusDays(2)));
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
    public Map<String, Object> listOrders() {
        log.info("获取订单列表");
        Map<String, Object> result = new HashMap<>();
        result.put("service", serviceName);
        result.put("port", port);
        result.put("data", new ArrayList<>(orderStore.values()));
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getOrder(@PathVariable Long id) {
        log.info("获取订单详情: {}", id);
        Map<String, Object> result = new HashMap<>();
        result.put("service", serviceName);
        result.put("port", port);
        result.put("data", orderStore.get(id));
        return result;
    }

    @PostMapping
    public Map<String, Object> createOrder(@RequestBody Order order) {
        log.info("创建订单: {}", order);
        Long id = (long) (orderStore.size() + 1);
        order.setId(id);
        order.setOrderNo("ORD-2024-" + String.format("%03d", id));
        order.setCreateTime(LocalDateTime.now());
        order.setStatus("PENDING");
        orderStore.put(id, order);

        Map<String, Object> result = new HashMap<>();
        result.put("service", serviceName);
        result.put("port", port);
        result.put("message", "订单创建成功");
        result.put("data", order);
        return result;
    }

    @PutMapping("/{id}/status")
    public Map<String, Object> updateOrderStatus(@PathVariable Long id, @RequestParam String status) {
        log.info("更新订单状态: {}, status: {}", id, status);
        Order order = orderStore.get(id);
        if (order != null) {
            order.setStatus(status);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("service", serviceName);
        result.put("port", port);
        result.put("message", "订单状态更新成功");
        result.put("data", order);
        return result;
    }

    @GetMapping("/user/{userId}")
    public Map<String, Object> getUserOrders(@PathVariable Long userId) {
        log.info("获取用户订单: {}", userId);
        List<Order> userOrders = new ArrayList<>();
        for (Order order : orderStore.values()) {
            if (order.getUserId().equals(userId)) {
                userOrders.add(order);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("service", serviceName);
        result.put("port", port);
        result.put("userId", userId);
        result.put("data", userOrders);
        return result;
    }

    @GetMapping("/stats")
    public Map<String, Object> getOrderStats() {
        Map<String, Object> result = new HashMap<>();
        result.put("service", serviceName);
        result.put("port", port);
        result.put("totalOrders", orderStore.size());
        result.put("totalAmount", orderStore.values().stream()
                .map(Order::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        result.put("statusCount", getStatusCount());
        return result;
    }

    private Map<String, Integer> getStatusCount() {
        Map<String, Integer> statusCount = new HashMap<>();
        for (Order order : orderStore.values()) {
            statusCount.merge(order.getStatus(), 1, Integer::sum);
        }
        return statusCount;
    }

    @Data
    public static class Order {
        private Long id;
        private String orderNo;
        private Long userId;
        private BigDecimal amount;
        private String status;
        private LocalDateTime createTime;

        public Order() {}

        public Order(Long id, String orderNo, Long userId, BigDecimal amount, String status, LocalDateTime createTime) {
            this.id = id;
            this.orderNo = orderNo;
            this.userId = userId;
            this.amount = amount;
            this.status = status;
            this.createTime = createTime;
        }
    }
}
