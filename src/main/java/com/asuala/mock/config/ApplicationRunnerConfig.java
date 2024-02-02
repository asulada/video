package com.asuala.mock.config;

import cn.hutool.core.collection.ListUtil;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.asuala.mock.es.Es8Client;
import com.asuala.mock.es.entity.FileInfoEs;
import com.asuala.mock.file.monitor.utils.MonitorFileUtil;
import com.asuala.mock.file.monitor.vo.FileTreeNode;
import com.asuala.mock.mapper.FileInfoMapper;
import com.asuala.mock.service.IndexService;
import com.asuala.mock.utils.CPUUtils;
import com.asuala.mock.utils.CacheUtils;
import com.asuala.mock.vo.FileInfo;
import com.asuala.mock.vo.Index;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description:
 * @create: 2020/07/16
 **/
@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationRunnerConfig implements ApplicationRunner {

    private final IndexService indexService;
    private final FileInfoMapper fileInfoMapper;

    @Autowired(required = false)
    private Es8Client es8Client;
    @Value("${down.rebuldFlag:false}")
    private boolean rebuldFlag;
    @Value("${down.downRole}")
    private boolean downRole;
    @Value("${down.volumeNo:'d:'}")
    private String volumeNo;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void run(ApplicationArguments args) throws Exception {
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

            //TODO 2022-09-18: ElasticSearch 创建索引
            try {
                es8Client.createIndexSettingsMappings(FileInfoEs.class);
            } catch (Exception e) {
                log.error("连接es创建索引失败", e);
            }
        }

        if (rebuldFlag) {
            //TODO-asuala 2024-02-02: 删除表数据
            fileInfoMapper.delete(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getIndex, CacheUtils.index));

            TreeMap<Long, FileTreeNode> map = MonitorFileUtil.buildFileInfo(volumeNo);
            List<FileInfo> fileInfo = MonitorFileUtil.getFileInfo(map, volumeNo);
            List<List<FileInfo>> split = ListUtil.split(fileInfo, 5000);
            for (List<FileInfo> fileInfos : split) {
                fileInfoMapper.batchInsert(fileInfos);
            }


            //TODO-asuala 2024-02-01: 处理重复文件
            List<String> names = fileInfoMapper.findNameByIndex(CacheUtils.index);
            List<Long> delIds = new ArrayList<>();

            for (String name : names) {
                List<FileInfo> list = fileInfoMapper.selectList(new LambdaQueryWrapper<FileInfo>().select(FileInfo::getId, FileInfo::getPath).eq(FileInfo::getName, name).eq(FileInfo::getIndex, CacheUtils.index));
//            AtomicLongArray atomicIdArr = new AtomicLongArray();
                Map<String, List<FileInfo>> listMap = list.stream().collect(Collectors.groupingBy(FileInfo::getPath));
                for (Map.Entry<String, List<FileInfo>> entry : listMap.entrySet()) {
                    if (entry.getValue().size() > 1) {
                        for (int i = 1; i < entry.getValue().size(); i++) {
                            delIds.add(entry.getValue().get(i).getId());
                        }
                    }
                }

            }

            if (delIds.size() > 0) {
                fileInfoMapper.deleteBatchIds(delIds);
            }

            es8Client.delQuery(Query.of(q -> q.match(m -> m.query(CacheUtils.index).field("index"))), FileInfoEs.class);
            //TODO-asuala 2024-02-01: es添加数据
            Page<FileInfo> page = new Page<>(1, 10000);
            List<FileInfo> fileInfos = fileInfoMapper.selectList(page, new LambdaQueryWrapper<FileInfo>().orderByAsc(FileInfo::getId));
            List<FileInfoEs> listEs = fileInfos.stream().map(item -> FileInfoEs.builder().id(item.getId()).changeTime(item.getChangeTime()).index(item.getIndex()).
                    path(item.getPath()).size(item.getSize()).name(item.getName()).suffix(item.getSuffix()).build()).collect(Collectors.toList());
            es8Client.addDatas(listEs, false);
        }
        log.info("初始化结束 客户端号: {}", CacheUtils.index);

    }

}