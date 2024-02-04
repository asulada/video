package com.asuala.mock.service;

import cn.hutool.core.collection.CollectionUtil;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.asuala.mock.annotation.Timer;
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
@ConditionalOnProperty(prefix = "down", name = "server", havingValue = "true")
public class ServerService {

    private final FileInfoMapper fileInfoMapper;
    @Autowired(required = false)
    private Es8Client es8Client;

    @Timer(value = "重置es")
    private void rebuildEs(int index) throws IOException {
        es8Client.delQuery(Query.of(q -> q.match(m -> m.query(index).field("index"))), FileInfoEs.class);
        //TODO-asuala 2024-02-01: es添加数据
        int pageNum = 1;
        while (true) {
            Page<FileInfo> page = new Page<>(pageNum++, 10000);
            List<FileInfo> fileInfos = fileInfoMapper.selectList(page, new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getIndex, index).orderByAsc(FileInfo::getId));
            if (fileInfos.size() == 0) {
                break;
            }
            List<FileInfoEs> listEs = fileInfos.stream().map(item -> FileInfoEs.builder().id(item.getId()).changeTime(item.getChangeTime()).index(item.getIndex()).
                    path(item.getPath()).size(item.getSize()).name(item.getName()).suffix(item.getSuffix()).build()).collect(Collectors.toList());
            es8Client.addDatas(listEs, false);
        }
    }

    @Timer("处理数据库重复文件")
    private void delRepearFileInfo(int index) {
        List<Map<String, String>> maps = fileInfoMapper.findNameByIndex(index);
        List<Long> delIds = new ArrayList<>();
        List<List<Map<String, String>>> split = CollectionUtil.split(maps, 20000);
        ThreadPoolExecutor executor = ThreadPoolExecutorUtils.getThreadPoolExecutorInstance();
        CopyOnWriteArrayList<Long> copy = new CopyOnWriteArrayList();
        for (List<Map<String, String>> mapList : split) {
            executor.execute(() -> {
                for (Map<String, String> map : mapList) {
                    String[] paths = map.get("path").split(";");
                    String[] ids = map.get("id").split(";");
                    Set<String> pathSet = new HashSet<>();

                    for (int i = 0; i < paths.length; i++) {
                        String value = paths[i];
                        if (pathSet.contains(value)) {
                            copy.add(Long.parseLong(ids[i]));
                        } else {
                            pathSet.add(value);
                        }
                    }
                }
            });
        }
        executor.shutdown();
        try {
            while (!executor.awaitTermination(1, TimeUnit.SECONDS)) ;
        } catch (InterruptedException e) {
            log.error("线程池失效", e);
        }
        if (delIds.size() > 0) {
            fileInfoMapper.deleteBatchIds(delIds);
        }
    }

    public void rebuildData(RebuildReq req) throws IOException {
        //TODO-asuala 2024-02-01: 处理重复文件
        delRepearFileInfo(req.getIndex());
        //TODO-asuala 2024-02-03: 重置es
        rebuildEs(req.getIndex());
    }
}