package com.asuala.mock.config;

import com.asuala.mock.m3u8.utils.Constant;
import com.asuala.mock.service.IndexService;
import com.asuala.mock.task.CommonTask;
import com.asuala.mock.utils.CPUUtils;
import com.asuala.mock.vo.Index;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @description:
 * @create: 2020/07/16
 **/
@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationRunnerConfig implements ApplicationRunner {

    private final IndexService indexService;

    @Autowired(required = false)
    private CommonTask commonTask;

    @Value("${down.client}")
    private boolean client;



    @Override
    public void run(ApplicationArguments args) throws Exception {
        Index index = CPUUtils.getCpuId();
        MainConstant.systemInfo = index;
        addIndex(index);

        if (client) {
            commonTask.addTask();
        }

        log.info("初始化结束 客户端号: {}", Constant.index);

    }


    private void addIndex(Index index) {
        try {
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
            index.setId(one.getId());
            Constant.index = Integer.parseInt(one.getId().toString());
        } catch (Exception e) {
            log.error("获取cpuid失败", e);
        }
    }


}