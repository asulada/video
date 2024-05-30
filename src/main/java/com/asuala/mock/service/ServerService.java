package com.asuala.mock.service;

import cn.hutool.core.collection.CollectionUtil;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.asuala.mock.annotation.Timer;
import com.asuala.mock.config.SpringContextUtil;
import com.asuala.mock.es.Es8Client;
import com.asuala.mock.es.entity.FileInfoEs;
import com.asuala.mock.mapper.FileInfoMapper;
import com.asuala.mock.utils.ThreadPoolExecutorUtils;
import com.asuala.mock.vo.FileInfo;
import com.asuala.mock.vo.req.RebuildReq;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @description:
 * @create: 2024/02/03
 **/
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "file", name = "server.open", havingValue = "true")
public class ServerService {

    private final TimerService timerService;

    @Async("threadPoolTaskExecutor")
    public void rebuildData(RebuildReq req) throws IOException {
        //TODO-asuala 2024-02-01: 处理重复文件
        timerService.delRepearFileInfo(req.getIndex());
        //TODO-asuala 2024-02-03: 重置es
        timerService.rebuildEs(req.getIndex());
    }
}