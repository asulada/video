package com.asuala.mock.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 * @description:
 * @create: 2024/05/21
 **/
@Slf4j
@Component
public class ShutdownHandler implements ApplicationListener<ContextClosedEvent> {

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        // 执行退出前的清理工作
        log.info("Spring Boot应用程序正在关闭...");


        // 例如，关闭数据库连接、停止后台线程等
        // ...
        log.info("清理完成，应用程序已关闭。");
    }
}