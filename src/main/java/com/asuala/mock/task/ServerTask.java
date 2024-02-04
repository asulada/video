package com.asuala.mock.task;

import com.asuala.mock.m3u8.utils.Constant;
import com.asuala.mock.service.IndexService;
import com.asuala.mock.service.RecordPageService;
import com.asuala.mock.vo.Index;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @description:
 * @create: 2024/01/26
 **/
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "down", name = "server", havingValue = "true")
public class ServerTask {

    private final RecordPageService recordPageService;
    private final IndexService indexService;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Scheduled(cron = "0 0 1 * * ?")
    public void deleteComplete() {
        recordPageService.deleteComplete();
    }

    @Scheduled(cron = "0 0/1 * * * ?")
    public void checkIndex() {
        LocalDateTime now = LocalDateTime.now();
        List<Index> list = indexService.list(new LambdaQueryWrapper<Index>().ge(Index::getUpdateTime, now.minusMinutes(40).format(formatter)));
        if (list.size() > 0) {
            Constant.index = Math.toIntExact(list.get(0).getId());
            log.debug("客户端信息 {}", Constant.index);
        } else {
            log.error("没有取到存活的客户端信息 !!!");
        }
    }

}