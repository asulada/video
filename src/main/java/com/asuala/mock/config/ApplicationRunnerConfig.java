package com.asuala.mock.config;

import com.asuala.mock.service.IndexService;
import com.asuala.mock.utils.CPUUtils;
import com.asuala.mock.utils.CacheUtils;
import com.asuala.mock.vo.Index;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * @description:
 * @create: 2020/07/16
 **/
@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationRunnerConfig implements ApplicationRunner {

    private final IndexService indexService;

    @Value("${down.downRole}")
    private boolean downRole;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void run(ApplicationArguments args) {
        if (downRole) {
            try {
                Index index = CPUUtils.getCpuId();
                Index one = indexService.findByCpuId(index.getCpuId());
                Date now = new Date();
                if (null == one) {
                    index.setCreateTime(now);
                    index.setUpdateTime(now);
                    indexService.save(index);
                    one = index;
                } else {
                    one.setDelFlag(0);
                    one.setUpdateTime(now);
                    indexService.updateById(one);
                }
                CacheUtils.index = Integer.parseInt(one.getId().toString());
            } catch (Exception e) {
                log.error("获取cpuid失败", e);
            }
        } else {
            LocalDateTime now = LocalDateTime.now();
            List<Index> list = indexService.list(new LambdaQueryWrapper<Index>().ge(Index::getUpdateTime, now.minusMinutes(40).format(formatter)));
            if (list.size() > 0) {
                CacheUtils.index = Math.toIntExact(list.get(0).getId());
                log.debug("客户端信息 {}", CacheUtils.index);
            } else {
                log.error("没有取到存活的客户端信息 !!!");
            }
        }
        log.info("初始化结束 客户端号: {}", CacheUtils.index);

    }

}