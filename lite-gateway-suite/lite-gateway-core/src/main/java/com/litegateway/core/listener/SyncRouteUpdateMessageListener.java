package com.litegateway.core.listener;

import com.litegateway.core.constants.RedisTypeConstants;
import com.litegateway.core.service.ConfigSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * 路由同步消息监听器
 * 监听 Redis 消息，同步路由、IP黑名单、白名单更新
 * 从旧项目迁移，包名从 com.jtyjy.gateway 改为 com.litegateway.core
 */
@Slf4j
@Component
public class SyncRouteUpdateMessageListener implements MessageListener, ApplicationEventPublisherAware {

    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private ConfigSyncService configSyncService;

    /**
     * 监听 Redis 消息
     * CHANNEL = "lite:gateway:sync:route:update"
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody());
        log.info("Received Redis message: {}", body);

        switch (body) {
            case RedisTypeConstants.ROUTE_UPDATE:
                // 同步路由
                log.info("Refreshing routes...");
                configSyncService.syncConfig();
                this.applicationEventPublisher.publishEvent(new RefreshRoutesEvent(this));
                break;
            case RedisTypeConstants.IP_UPDATE:
                // 同步 IP 黑名单
                log.info("Refreshing IP blacklist...");
                configSyncService.syncConfig();
                this.applicationEventPublisher.publishEvent(new DataIpRefreshEvent(this));
                break;
            case RedisTypeConstants.WHITE_LIST_UPDATE:
                // 同步白名单
                log.info("Refreshing whitelist...");
                configSyncService.syncConfig();
                this.applicationEventPublisher.publishEvent(new WhiteListRefreshEvent(this));
                break;
            default:
                log.warn("Unknown message type: {}", body);
        }
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}
